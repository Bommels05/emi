package dev.emi.emi.backport.java;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;

public class Set {

    public static <T> java.util.Set<T> of() {
        return ImmutableSet.of();
    }

    public static <T> java.util.Set<T> of(T... values) {
        return ImmutableSet.copyOf(values);
    }

    public static <T> java.util.Set<T> copyOf(Collection<T> set) {
        return ImmutableSet.copyOf(set);
    }

}
