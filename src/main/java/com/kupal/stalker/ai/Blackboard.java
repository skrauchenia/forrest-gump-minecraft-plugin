package com.kupal.stalker.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class Blackboard {
    private final Map<BlackboardKey<?>, Object> data = new HashMap<>();

    public <T> void put(BlackboardKey<T> key, T value) {
        data.put(key, value);
    }

    public <T> Optional<T> get(BlackboardKey<T> key) {
        Object value = data.get(key);
        if (value == null) return Optional.empty();
        return Optional.of(key.type().cast(value));
    }

    public <T> T getOrDefault(BlackboardKey<T> key, T defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public boolean contains(BlackboardKey<?> key) {
        return data.containsKey(key);
    }

    public void remove(BlackboardKey<?> key) {
        data.remove(key);
    }
}