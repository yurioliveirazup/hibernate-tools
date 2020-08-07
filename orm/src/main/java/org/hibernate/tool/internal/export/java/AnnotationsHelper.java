package org.hibernate.tool.internal.export.java;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.*;

import java.util.Iterator;

public class AnnotationsHelper {

    private BasicPOJOClass basicPOJOClass;

    public AnnotationsHelper(BasicPOJOClass basicPOJOClass) {
        this.basicPOJOClass = basicPOJOClass;
    }

    public String basicAnnotationsTo(Property property) {
        StringBuffer annotations = new StringBuffer( "    " );
        if(property.getValue() instanceof SimpleValue) {
            if (basicPOJOClass.hasVersionProperty())
                if (property.equals(basicPOJOClass.getVersionProperty()))
                    buildVersionAnnotation(annotations);
            String typeName = ((SimpleValue)property.getValue()).getTypeName();
            if("date".equals(typeName) || "java.sql.Date".equals(typeName)) {
                buildTemporalAnnotation( annotations, "DATE" );
            } else if ("timestamp".equals(typeName) || "java.sql.Timestamp".equals(typeName)) {
                buildTemporalAnnotation( annotations, "TIMESTAMP" );
            } else if ("time".equals(typeName) || "java.sql.Time".equals(typeName)) {
                buildTemporalAnnotation(annotations, "TIME");
            } //TODO: calendar etc. ?


        }

        return annotations.toString();
    }

    private StringBuffer buildVersionAnnotation(StringBuffer annotations) {
        String version = basicPOJOClass.importType("javax.persistence.Version");

        return annotations.append( "@" + version );
    }

    private StringBuffer buildTemporalAnnotation(StringBuffer annotations, String temporalTypeValue) {
        String temporal = basicPOJOClass.importType("javax.persistence.Temporal");
        String temporalType = basicPOJOClass.importType("javax.persistence.TemporalType");

        return annotations.append( "@" + temporal +"(" + temporalType + "." + temporalTypeValue + ")");
    }

    public String generateColumnAnnotations(Property property) {
        StringBuffer annotations = new StringBuffer( "    " );
        boolean insertable = property.isInsertable();
        boolean updatable = property.isUpdateable();
        if ( property.isComposite() ) {
            annotations.append( "@" + basicPOJOClass.importType("javax.persistence.AttributeOverrides") +"( {" );
            Component component = (Component) property.getValue();
            Iterator<?> subElements = component.getPropertyIterator();
            buildRecursiveAttributeOverride( subElements, null, property, annotations );
            annotations.setLength( annotations.length() - 2 );
            annotations.append( " } )" );
        }
        else {
            if ( property.getColumnSpan() == 1 ) {
                Selectable selectable = (Selectable) property.getColumnIterator().next();
                buildColumnAnnotation( selectable, annotations, insertable, updatable );
            }
            else {
                Iterator<?> columns = property.getColumnIterator();
                annotations.append("@").append( basicPOJOClass.importType("org.hibernate.annotations.Columns") ).append("( { " );
                while ( columns.hasNext() ) {
                    Selectable selectable = (Selectable) columns.next();

                    if ( selectable.isFormula() ) {
                        //TODO formula in multicolumns not supported by annotations
                        //annotations.append("/* TODO formula in multicolumns not supported by annotations */");
                    }
                    else {
                        annotations.append( "\n        " );
                        buildColumnAnnotation( selectable, annotations, insertable, updatable );
                        annotations.append( ", " );
                    }
                }
                annotations.setLength( annotations.length() - 2 );
                annotations.append( " } )" );
            }
        }
        return annotations.toString();
    }

    private void buildColumnAnnotation(Selectable selectable, StringBuffer annotations, boolean insertable, boolean updatable) {
        if ( selectable.isFormula() ) {
            annotations.append("@").append( basicPOJOClass.importType("org.hibernate.annotations.Formula") )
                    .append("(value=\"" ).append( selectable.getText() ).append( "\")" );
        }
        else {
            Column column = (Column) selectable;
            annotations.append( "@" + basicPOJOClass.importType("javax.persistence.Column") + "(name=\"" ).append( column.getName() ).append( "\"" );

            basicPOJOClass.appendCommonColumnInfo( annotations, column, insertable, updatable );

            if (column.getPrecision() != null) {
                annotations.append( ", precision=" ).append( column.getPrecision() );
            }
            if (column.getScale() != null) {
                annotations.append( ", scale=" ).append( column.getScale() );
            }
            else if (column.getLength() != null){
                annotations.append( ", length=" ).append( column.getLength() );
            }




            //TODO support secondary table
            annotations.append( ")" );
        }
    }

    private void buildRecursiveAttributeOverride(Iterator<?> subElements, String path, Property property, StringBuffer annotations) {
        while ( subElements.hasNext() ) {
            Property subProperty = (Property) subElements.next();
            if ( subProperty.isComposite() ) {
                if ( path != null ) {
                    path = path + ".";
                }
                else {
                    path = "";
                }
                path = path + subProperty.getName();
                Component component = (Component) subProperty.getValue();
                buildRecursiveAttributeOverride( component.getPropertyIterator(), path, subProperty, annotations );
            }
            else {
                Iterator<?> columns = subProperty.getColumnIterator();
                Selectable selectable = (Selectable) columns.next();
                if ( selectable.isFormula() ) {
                    //TODO formula in multicolumns not supported by annotations
                }
                else {
                    annotations.append( "\n        " ).append("@")
                            .append( basicPOJOClass.importType("javax.persistence.AttributeOverride") ).append("(name=\"" );
                    if ( path != null ) {
                        annotations.append( path ).append( "." );
                    }
                    annotations.append( subProperty.getName() ).append( "\"" )
                            .append( ", column=" );
                    buildColumnAnnotation(
                            selectable, annotations, subProperty.isInsertable(), subProperty.isUpdateable()
                    );
                    annotations.append( " ), " );
                }
            }
        }
    }

    public void addAnnotationsProperties(StringBuffer annotations, Column column, boolean insertable, boolean updatable) {
        if(column.isUnique()) {
            annotations.append( ", unique=" ).append( column.isUnique() );
        }
        if(!column.isNullable()) {
            annotations.append( ", nullable=" ).append( column.isNullable() );
        }

        if(!insertable) {
            annotations.append( ", insertable=" ).append( insertable );
        }

        if(!updatable) {
            annotations.append( ", updatable=" ).append( updatable );
        }

        String sqlType = column.getSqlType();
        if ( StringHelper.isNotEmpty( sqlType ) ) {
            annotations.append( ", columnDefinition=\"" ).append( sqlType ).append( "\"" );
        }
    }
}
