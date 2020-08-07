package org.hibernate.tool.internal.export.java;

import org.hibernate.mapping.*;
import org.hibernate.tool.internal.export.common.DefaultValueVisitor;

import java.util.HashMap;

class FieldsInitializer {

    private Cfg2JavaTool c2j;
    private BasicPOJOClass basicPOJOClass;
    private ImportContext importContext;

    public FieldsInitializer(Cfg2JavaTool c2j, BasicPOJOClass basicPOJOClass, ImportContext importContext) {
        this.c2j = c2j;
        this.basicPOJOClass = basicPOJOClass;
        this.importContext = importContext;
    }

    public String findInitializerTo(Property property, boolean useGenerics) {
        if(basicPOJOClass.hasMetaAttribute(property, "default-value")) {
            return MetaAttributeHelper.getMetaAsString( property.getMetaAttribute( "default-value" ) );
        }
        if(c2j.getJavaTypeName(property, false)==null) {
            throw new IllegalArgumentException();
        } else if (property.getValue() instanceof Collection) {
            Collection col = (Collection) property.getValue();

            DefaultInitializor initialization = (DefaultInitializor) col.accept(new DefaultValueVisitor(true) {

                public Object accept(Bag o) {
                    return new DefaultInitializor("java.util.ArrayList", true);
                }

                public Object accept(org.hibernate.mapping.List o) {
                    return new DefaultInitializor("java.util.ArrayList", true);
                }

                public Object accept(org.hibernate.mapping.Map o) {
                    if(o.isSorted()) {
                        return new DefaultInitializor("java.util.TreeMap", false);
                    } else {
                        return new DefaultInitializor("java.util.HashMap", true);
                    }
                }

                public Object accept(IdentifierBag o) {
                    return new DefaultInitializor("java.util.ArrayList", true);
                }

                public Object accept(Set o) {
                    if(o.isSorted()) {
                        return new DefaultInitializor("java.util.TreeSet", false);
                    } else {
                        return new DefaultInitializor("java.util.HashSet", true);
                    }
                }


                public Object accept(PrimitiveArray o) {
                    return null; // TODO: default init for arrays ?
                }

                public Object accept(Array o) {
                    return null;// TODO: default init for arrays ?
                }

            });

            if(initialization!=null) {
                String comparator = null;
                String decl = null;

                if(col.isSorted()) {
                    comparator = col.getComparatorClassName();
                }

                if(useGenerics) {
                    decl = c2j.getGenericCollectionDeclaration((Collection) property.getValue(), true, importContext);
                }
                return initialization.getDefaultValue(comparator, decl, basicPOJOClass);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    static private class DefaultInitializor {

        private final String type;
        private final boolean initToZero;

        public DefaultInitializor(String type, boolean initToZero) {
            this.type = type;
            this.initToZero = initToZero;
        }

        public String getDefaultValue(String comparator, String genericDeclaration, ImportContext importContext) {
            StringBuffer val = new StringBuffer("new " + importContext.importType(type));
            if(genericDeclaration!=null) {
                val.append(genericDeclaration);
            }

            val.append("(");
            if(comparator!=null) {
                val.append("new ");
                val.append(importContext.importType(comparator));
                val.append("()");
                if(initToZero) val.append(",");
            }
            if(initToZero) {
                val.append("0");
            }
            val.append(")");
            return val.toString();
        }

    }

    static java.util.Map<String, DefaultInitializor> defaultInitializors = new HashMap<String, DefaultInitializor>();
    static {
        defaultInitializors.put("java.util.List", new DefaultInitializor("java.util.ArrayList", true));
        defaultInitializors.put("java.util.Map", new DefaultInitializor("java.util.HashMap", true));
        defaultInitializors.put("java.util.Set", new DefaultInitializor("java.util.HashSet",true));
        defaultInitializors.put("java.util.SortedSet", new DefaultInitializor("java.util.TreeSet", false));
        defaultInitializors.put("java.util.SortedMap", new DefaultInitializor("java.util.TreeMap", false));
    }
}
