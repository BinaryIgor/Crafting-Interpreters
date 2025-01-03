package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Environment {
    private final Environment enclosing;
    private final Map<String, Object> values = new HashMap<>();

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Environment() {
        this(null);
    }

    void define(String name, Object value) {
        values.put(name, value);
    }

    Object get(Token name) {
        return Optional.ofNullable(values.get(name.lexeme()))
            .or(() -> Optional.ofNullable(enclosing).map(enc -> enc.get(name)))
            .orElseThrow(() -> undefinedVariableError(name));
    }

    private RuntimeError undefinedVariableError(Token name) {
        return new RuntimeError(name, "Undefined variable '%s'".formatted(name.lexeme()));
    }

    Object getAt(int distance, String name) {
        return ancestor(distance).values.get(name);
    }

    Environment ancestor(int distance) {
        var environment = this;
        for (var i = 0; i < distance; i++) {
            environment = environment.enclosing;
        }
        return environment;
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme())) {
            values.put(name.lexeme(), value);
            return;
        }
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        throw undefinedVariableError(name);
    }

    void assignAt(int distance, Token name, Object value) {
        ancestor(distance).values.put(name.lexeme(), value);
    }
}
