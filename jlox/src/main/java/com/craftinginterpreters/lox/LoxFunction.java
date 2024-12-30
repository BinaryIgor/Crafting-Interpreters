package com.craftinginterpreters.lox;

import java.util.List;
import java.util.stream.IntStream;

public class LoxFunction implements LoxCallable {

    private final List<Token> params;
    private final List<Stmt> body;
    private final String name;
    private final Environment closure;
    private final boolean initializer;

    LoxFunction(List<Token> params, List<Stmt> body, String name, Environment closure, boolean initializer) {
        this.params = params;
        this.body = body;
        this.name = name;
        this.closure = closure;
        this.initializer = initializer;
    }

    LoxFunction(Stmt.Function function, Environment closure, boolean initializer) {
        this(function.params, function.body, function.name.lexeme(), closure, initializer);
    }

    LoxFunction(Expr.Function function, Environment closure, boolean initializer) {
        this(function.params, function.body, "anonymous", closure, initializer);
    }

    @Override
    public int arity() {
        return params.size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        var env = new Environment(closure);

        IntStream.range(0, arity())
            .forEach(i -> {
                var paramName = params.get(i).lexeme();
                var paramValue = arguments.get(i);
                env.define(paramName, paramValue);
            });

        try {
            interpreter.executeBlock(body, env);
            return initializer ? thisFromClosure() : null;
        } catch (Interpreter.ReturnException e) {
            if (initializer) {
                return thisFromClosure();
            }
            return e.value;
        }
    }

    private Object thisFromClosure() {
        return closure.getAt(0, "this");
    }

    LoxFunction bind(LoxInstance instance) {
        var environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(params, body, name, environment, initializer);
    }

    @Override
    public String toString() {
        return "<fn %s>".formatted(name);
    }
}
