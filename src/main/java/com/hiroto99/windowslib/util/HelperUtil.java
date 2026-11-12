package com.hiroto99.windowslib.util;

public class HelperUtil {
    @SafeVarargs
    public static <T> boolean orEquals(T obj, T... toObj) {
        T[] toObjectList = toObj;
        for (T objT : toObjectList) {
            if (obj == objT) {
                return true;
            }
        }
        return false;
    }
}
