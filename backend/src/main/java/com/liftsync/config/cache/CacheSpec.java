package com.liftsync.config.cache;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Binds a logical cache name to the concrete Java type stored in it.
 *
 * Having a specific type per cache resolves the issue of storing a class path as metadata in the cache,
 * which is brittle and can lead to deserialization issues if class names change.
 * Instead, we can use the JavaType to guide Jackson's deserialization without relying on class name strings.
 *
 * @param <T> compile-time hint of the cached value type (informational; Jackson only needs the JavaType)
 */
public record CacheSpec<T>(String name, JavaType javaType) {

    private static final TypeFactory TF = TypeFactory.defaultInstance();

    public static <T> CacheSpec<T> of(String name, Class<T> type) {
        return new CacheSpec<>(name, TF.constructType(type));
    }

    public static <E> CacheSpec<List<E>> ofList(String name, Class<E> elementType) {
        return new CacheSpec<>(name, TF.constructParametricType(List.class, elementType));
    }

    public static <K, V> CacheSpec<Map<K, V>> ofMap(String name, Class<K> keyType, Class<V> valueType) {
        return new CacheSpec<>(name, TF.constructParametricType(Map.class, keyType, valueType));
    }
}
