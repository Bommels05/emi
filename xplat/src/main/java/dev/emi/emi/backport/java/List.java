package dev.emi.emi.backport.java;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collection;

public class List {

    public static <T> java.util.List<T> of() {
        return ImmutableList.of();
    }

    public static <T> java.util.List<T> of(T... values) {
        return ImmutableList.copyOf(values);
    }

    public static <T> java.util.List<T> copyOf(Collection<T> list) {
        return ImmutableList.copyOf(list);
    }

}
