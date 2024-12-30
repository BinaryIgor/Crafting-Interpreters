package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class LoxInstance {
    private final LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }

    Object get(Token name) {
        return Optional.ofNullable(fields.get(name.lexeme()))
            .or(() -> Optional.ofNullable(klass.findMethod(name.lexeme()))
                .map(m -> m.bind(this)))
            .orElseThrow(() -> new RuntimeError(name, "Undefined property '%s'".formatted(name.lexeme())));
    }

    void set(Token name, Object value) {
        fields.put(name.lexeme(), value);
    }
}
