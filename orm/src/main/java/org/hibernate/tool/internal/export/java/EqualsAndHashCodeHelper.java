package org.hibernate.tool.internal.export.java;

import org.hibernate.mapping.Property;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class EqualsAndHashCodeHelper {

    private Cfg2JavaTool c2j;
    private BasicPOJOClass basicPOJOClass;
    private boolean useGenerics;

    public EqualsAndHashCodeHelper(Cfg2JavaTool c2j, BasicPOJOClass basicPOJOClass, boolean useGenerics) {
        this.c2j = c2j;
        this.basicPOJOClass = basicPOJOClass;
        this.useGenerics = useGenerics;
    }

    public String generateEquals(String thisName, String otherName, Iterator<Property> allPropertiesIterator) {
        StringBuilder buf = new StringBuilder();
        while ( allPropertiesIterator.hasNext() ) {
            Property property = (Property) allPropertiesIterator.next();
            if ( buf.length() > 0 ) buf.append( "\n && " );
            String javaTypeName = c2j.getJavaTypeName( property, useGenerics, basicPOJOClass );
            buf.append(
                    internalgenerateEquals(
                            javaTypeName, thisName + "." + basicPOJOClass.getGetterSignature( property ) + "()",
                            otherName + "." + basicPOJOClass.getGetterSignature( property ) + "()")
            );
        }

        if ( buf.length() == 0 ) {
            return "false";
        }
        else {
            return buf.toString();
        }
    }

    private String internalgenerateEquals(String typeName, String lh, String rh) {
        if ( c2j.isPrimitive( typeName ) ) {
            return "(" + lh + "==" + rh + ")";
        }
        else {
            if(useCompareTo( typeName )) {
                return "( (" + lh + "==" + rh + ") || ( " + lh + "!=null && " + rh + "!=null && " + lh + ".compareTo(" + rh + ")==0 ) )";
            } else {
                if(typeName.endsWith("[]")) {
                    return "( (" + lh + "==" + rh + ") || ( " + lh + "!=null && " + rh + "!=null && " + basicPOJOClass.importType("java.util.Arrays") + ".equals(" + lh + ", " + rh + ") ) )";
                } else {
                    return "( (" + lh + "==" + rh + ") || ( " + lh + "!=null && " + rh + "!=null && " + lh + ".equals(" + rh + ") ) )";
                }
            }

        }
    }

    private boolean useCompareTo(String javaTypeName) {
        // Fix for HBX-400
        return "java.math.BigDecimal".equals(javaTypeName);
    }

    public boolean needsEqualsAndHashCode(Iterator<?> iter) {
        while ( iter.hasNext() ) {
            Property element = (Property) iter.next();
            if ( usePropertyInEquals( element ) ) {
                return true;
            }
        }
        return false;
    }

    private boolean usePropertyInEquals(Property property) {
        boolean hasEqualsMetaAttribute = c2j.hasMetaAttribute(property, "use-in-equals");
        boolean useInEquals = c2j.getMetaAsBool( property, "use-in-equals" );

        if(property.isNaturalIdentifier()) {
            return !hasEqualsMetaAttribute || useInEquals;
        }

        return useInEquals;
    }

    public Iterator<Property> findProperties(Iterator<Property> iter) {
        List<Property> properties = new ArrayList<Property>();

        while ( iter.hasNext() ) {
            Property element = iter.next();
            if ( usePropertyInEquals(element) ) {
                properties.add( element );
            }
        }

        return properties.iterator();
    }

    public String generateHashCode(Property property, String result, String thisName, boolean jdk5) {
        StringBuffer buf = new StringBuffer();
        if ( c2j.getMetaAsBool( property, "use-in-equals" ) ) {
            String javaTypeName = c2j.getJavaTypeName( property, jdk5, basicPOJOClass );
            boolean isPrimitive = c2j.isPrimitive( javaTypeName );
            if ( isPrimitive ) {
                buf.append( result )
                        .append( " = 37 * " )
                        .append( result )
                        .append( " + " );
                String thisValue = thisName + "." + basicPOJOClass.getGetterSignature( property ) + "()";
                if("char".equals(javaTypeName)||"int".equals(javaTypeName)||"short".equals(javaTypeName)||"byte".equals(javaTypeName)) {
                    buf.append( thisValue );
                } else if("boolean".equals(javaTypeName)) {
                    buf.append("(" + thisValue + "?1:0)");
                } else {
                    buf.append( "(int) ");
                    buf.append( thisValue );
                }
                buf.append(";");
            }
            else {
                if(javaTypeName.endsWith("[]")) {
                    if(jdk5) {
                        buf.append( result )
                                .append( " = 37 * " )
                                .append( result )
                                .append( " + " );
                        buf.append( "( " )
                                .append( basicPOJOClass.getGetterSignature( property ) )
                                .append( "() == null ? 0 : " + basicPOJOClass.importType("java.util.Arrays") + ".hashCode(" )
                                .append( thisName )
                                .append( "." )
                                .append( basicPOJOClass.getGetterSignature( property ) )
                                .append( "())" )
                                .append( " )" )
                                .append(";");
                    }
                    else {
                        buf.append(internalGenerateArrayHashcode(property, javaTypeName, result, thisName));
                    }
                } else {
                    buf.append( result )
                            .append( " = 37 * " )
                            .append( result )
                            .append( " + " );
                    buf.append( "( " )
                            .append(basicPOJOClass.getGetterSignature( property ) )
                            .append( "() == null ? 0 : " )
                            .append( thisName )
                            .append( "." )
                            .append(basicPOJOClass.getGetterSignature( property ) )
                            .append( "()" )
                            .append( ".hashCode()" )
                            .append( " )" )
                            .append(";");
                }
            }
        }
        return buf.toString();
    }

    private String internalGenerateArrayHashcode(Property property, String javaTypeName, String result, String thisName)
    {
        StringBuffer buf = new StringBuffer();

        String propertyHashVarName = property.getName() + "Hashcode";
        String propertyArrayName = property.getName() + "Property";

//		int propertyHash = 0;
        buf.append( "int ")
                .append( propertyHashVarName )
                .append( " = 0;\n" );

//		type[] proterty = getProperty();
        buf.append( "         " )
                .append( javaTypeName )
                .append( " " )
                .append( propertyArrayName )
                .append( " = " )
                .append( thisName )
                .append( "." )
                .append( basicPOJOClass.getGetterSignature( property ) )
                .append( "();\n");

//		if(property != null) {
        buf.append( "         if(" )
                .append( propertyArrayName )
                .append( " != null) {\n" );

//		propertyHash = 1;
        buf.append( "             " )
                .append( propertyHashVarName )
                .append( " = 1;\n" );

//		for (int i=0; i<property.length; i++)
        javaTypeName.replaceAll("\\[\\]", "");
        buf.append( "             for (int i=0; i<" )
                .append( propertyArrayName )
                .append( ".length; i++) {\n" );

        if(javaTypeName.startsWith("long")) {
//			int elementHash = (int)(propertyArray[i] ^ (propertyArray[i] >>> 32));
            buf.append( "                 int elementHash = (int)(" )
                    .append( propertyArrayName )
                    .append( "[i] ^ (" )
                    .append( propertyArrayName )
                    .append( "[i] >>> 32));\n" );

//			propertyHash = 37 * propertyHash + elementHash;
            buf.append( "                 " )
                    .append( propertyHashVarName )
                    .append( " = 37 * " )
                    .append( propertyHashVarName )
                    .append( " + elementHash;\n" );
        } else if(javaTypeName.startsWith("boolean")) {
//			propertyHash = 37 * propertyHash + (propertyArray[i] ? 1231 : 1237);
            buf.append( "                 " )
                    .append( propertyHashVarName )
                    .append( " = 37 * " )
                    .append( propertyHashVarName )
                    .append( " + (" )
                    .append( propertyArrayName )
                    .append( "[i] ? 1231 : 1237);\n" );
        } else if(javaTypeName.startsWith("float")) {
//			propertyHash = 37 * propertyHash + Float.floatToIntBits(propertyArray[i]);
            buf.append( "                 " )
                    .append( propertyHashVarName )
                    .append( " = 37 * " )
                    .append( propertyHashVarName )
                    .append( " + Float.floatToIntBits(" )
                    .append( propertyArrayName )
                    .append( "[i]);\n" );
        } else if(javaTypeName.startsWith("double")) {
//			long bits = Double.doubleToLongBits(propertyArray[i]);
            buf.append( "                 long bits = Double.doubleToLongBits(" )
                    .append( propertyArrayName )
                    .append( "[i]);\n" );

//			propertyHash = 37 * propertyHash + (int)(bits ^ (bits >>> 32));
            buf.append( "                 " )
                    .append( propertyHashVarName )
                    .append( " = 37 * " )
                    .append( propertyHashVarName )
                    .append( " + (int)(bits ^ (bits >>> 32));\n" );
        } else if(javaTypeName.startsWith("int")
                || javaTypeName.startsWith("short")
                || javaTypeName.startsWith("char")
                || javaTypeName.startsWith("byte")) {
//			propertyHash = 37 * propertyHash + propertyArray[i];
            buf.append( "                 " )
                    .append( propertyHashVarName )
                    .append( " = 37 * " )
                    .append( propertyHashVarName )
                    .append( " + " )
                    .append( propertyArrayName )
                    .append( "[i];\n" );
        } else {// Object[]
//			propertyHash = 37 * propertyHash + propertyArray[i].hashCode();
            buf.append( "                 " )
                    .append( propertyHashVarName )
                    .append( " = 37 * " )
                    .append( propertyHashVarName )
                    .append( " + " )
                    .append( propertyArrayName )
                    .append( "[i].hashCode();\n" );
        }

        buf.append( "             }\n" );
        buf.append( "         }\n\n" );

//		result = 37 * result + arrayHashcode;
        buf.append( "         " )
                .append( result )
                .append( " = 37 * " )
                .append( result )
                .append( " + " )
                .append( propertyHashVarName )
                .append( ";\n" );

        return buf.toString();
    }
}
