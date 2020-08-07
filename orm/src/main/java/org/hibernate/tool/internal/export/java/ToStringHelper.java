package org.hibernate.tool.internal.export.java;

import org.hibernate.mapping.Property;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ToStringHelper {

    private Cfg2JavaTool c2j;

    public ToStringHelper(Cfg2JavaTool c2j) {
        this.c2j = c2j;
    }

    public Iterator<Property> findToStringProperties(Iterator<Property> iter) {
        List<Property> properties = new ArrayList<Property>();

        while ( iter.hasNext() ) {
            Property element = iter.next();
            if ( c2j.getMetaAsBool( element, "use-in-tostring" ) ) {
                properties.add( element );
            }
        }

        return properties.iterator();
    }

    public boolean needsToString(Iterator<Property> iter) {
        while ( iter.hasNext() ) {
            Property element = (Property) iter.next();
            if ( c2j.getMetaAsBool( element, "use-in-tostring" ) ) {
                return true;
            }
        }
        return false;
    }
}
