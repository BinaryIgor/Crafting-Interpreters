package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LoxClass implements LoxCallable {

    final String name;
    private final Map<String, LoxFunction> methods;

    LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    @Override
    public int arity() {
        return findInitializer().map(LoxFunction::arity).orElse(0);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var instance = new LoxInstance(this);
        findInitializer().ifPresent(init -> init.bind(instance).call(interpreter, arguments));
        return instance;
    }

    private Optional<LoxFunction> findInitializer() {
        return Optional.ofNullable(findMethod("init"));
    }

    LoxFunction findMethod(String name) {
        return methods.get(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
