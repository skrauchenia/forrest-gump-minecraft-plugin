package com.kupal.stalker.util;

public enum IntComparison {
    GT(">") {
        @Override
        public boolean test(int a, int b) {
            return a > b;
        }
    },
    GE(">=") {
        @Override
        public boolean test(int a, int b) {
            return a >= b;
        }
    },
    LT("<") {
        @Override
        public boolean test(int a, int b) {
            return a < b;
        }
    },
    LE("<=") {
        @Override
        public boolean test(int a, int b) {
            return a <= b;
        }
    },
    EQ("==") {
        @Override
        public boolean test(int a, int b) {
            return a == b;
        }
    },
    NE("!=") {
        @Override
        public boolean test(int a, int b) {
            return a != b;
        }
    };

    private final String symbol;

    IntComparison(String symbol) {
        this.symbol = symbol;
    }

    public abstract boolean test(int a, int b);

    public String symbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol;
    }
}