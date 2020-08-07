package org.hibernate.tool.internal.export.java;

import org.hibernate.mapping.MetaAttribute;

import java.util.Iterator;

class ImportHelper {

    private ImportContext importContext;

    public ImportHelper(ImportContext importContext) {
        this.importContext = importContext;
    }


    public void initializeImports(MetaAttribute metaAttribute) {
        if(metaAttribute!=null) {
            for (Object o : metaAttribute.getValues()) {
                String element = (String) o;
                importContext.importType(element);
            }
        }
    }
}
