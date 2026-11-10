package com.hiroto99.windowslib.util;

import java.util.ArrayList;
import java.util.List;

public class BiListArray {
    public static <T> T[] toArray(List<T> list) {
        T[] array = (T[]) new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static <T> List<T> toList(T[] array) {
        return new ArrayList<>(List.of(array));
    }
}
