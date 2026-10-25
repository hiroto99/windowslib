package com.hiroto99.windowslib.util;

import java.util.List;

public class ListToArray {
    public <T, U extends List<T>> T[] of(U list) {
        T[] array = (T[]) new Object[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
