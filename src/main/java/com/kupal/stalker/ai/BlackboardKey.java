package com.kupal.stalker.ai;

public record BlackboardKey<T>(String name, Class<T> type) {
    public static <T> BlackboardKey<T> of(String name, Class<T> type) {
        return new BlackboardKey<>(name, type);
    }
}