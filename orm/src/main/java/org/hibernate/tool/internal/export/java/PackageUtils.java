package org.hibernate.tool.internal.export.java;

class PackageUtils {

    public static String packageDeclaration(String pkgName) {
        if (pkgName!=null && pkgName.trim().length()!=0 ) {
            return "package " + pkgName + ";";
        }
        else {
            return "// default package";
        }
    }
}
