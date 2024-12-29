package com.craftinginterpreters.lox;

import java.util.List;
import java.util.stream.IntStream;

public class LoxFunction implements LoxCallable {

    private final Stmt.Function declaration;
    private final Environment closure;

    LoxFunction(Stmt.Function declaration, Environment closure) {
        this.declaration = declaration;
        this.closure = closure;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var env = new Environment(closure);

        IntStream.range(0, arity())
            .forEach(i -> {
                var paramName = declaration.params.get(i).lexeme();
                var paramValue = arguments.get(i);
                env.define(paramName, paramValue);
            });

        try {
            interpreter.executeBlock(declaration.body, env);
            return null;
        } catch (Interpreter.ReturnException e) {
            return e.value;
        }
    }

    @Override
    public String toString() {
        return "<fn %s>".formatted(declaration.name.lexeme());
    }
}
