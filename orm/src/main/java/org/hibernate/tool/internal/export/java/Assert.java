package org.hibernate.tool.internal.export.java;

class Assert {
    public static <T>  T notNull(T obj, String msg) {
        if (obj == null) {
            throw new IllegalArgumentException(msg);
        }

        return obj;
    }
}
