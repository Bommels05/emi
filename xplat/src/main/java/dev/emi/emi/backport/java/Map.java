package dev.emi.emi.backport.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class Map {

    public static <K, V> java.util.Map<K, V> of() {
        return ImmutableMap.of();
    }

    public static <K, V> java.util.Map<K, V> of(K k, V v) {
        return ImmutableMap.of(k, v);
    }

}
