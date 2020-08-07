package org.hibernate.tool.internal.export.java;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.MetaAttribute;
import org.hibernate.mapping.Property;
import org.hibernate.tuple.GenerationTiming;

import java.util.Iterator;

class BeanUtils {

    public static String capitalize(String fieldName) {
        if ( fieldName == null || fieldName.length() == 0 ) {
            return fieldName;
        }

        if ( fieldName.length() > 1 && Character.isUpperCase( fieldName.charAt( 1 ) ) ) {
            return fieldName;
        }
        char[] chars = fieldName.toCharArray();
        chars[0] = Character.toUpperCase( chars[0] );
        return new String( chars );
    }

    //TODO defaultModifiers
    public static String getModifiers(Property property, String modifierName, String defaultModifiers) {
        MetaAttribute override = property.getMetaAttribute( modifierName );
        if ( override != null ) {
            return MetaAttributeHelper.getMetaAsString( override );
        }

        return defaultModifiers;
    }

    public static boolean requiredInConstructor(Property field, BasicPOJOClass basicPOJOClass) {
        if(basicPOJOClass.hasMetaAttribute(field, "default-value")) {
            return false;
        }
        if(field.getValue()!=null) {
            if (!field.isOptional() && (field.getValueGenerationStrategy() == null || field.getValueGenerationStrategy().getGenerationTiming().equals(GenerationTiming.NEVER))) {
                return true;
            } else if (field.getValue() instanceof Component) {
                Component c = (Component) field.getValue();
                Iterator<?> it = c.getPropertyIterator();
                while ( it.hasNext() ) {
                    Property prop = (Property) it.next();
                    if(requiredInConstructor(prop, basicPOJOClass)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
