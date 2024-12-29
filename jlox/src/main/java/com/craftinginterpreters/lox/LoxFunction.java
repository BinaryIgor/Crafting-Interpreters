package com.craftinginterpreters.lox;

import java.util.List;
import java.util.stream.IntStream;

public class LoxFunction implements LoxCallable {

    private final List<Token> params;
    private final List<Stmt> body;
    private final String name;
    private final Environment closure;

    LoxFunction(List<Token> params, List<Stmt> body, String name, Environment closure) {
        this.params = params;
        this.body = body;
        this.name = name;
        this.closure = closure;
    }

    LoxFunction(Stmt.Function function, Environment closure) {
        this(function.params, function.body, function.name.lexeme(), closure);
    }

    LoxFunction(Expr.Function function, Environment closure) {
        this(function.params, function.body, "anonymous", closure);
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
            return null;
        } catch (Interpreter.ReturnException e) {
            return e.value;
        }
    }

    @Override
    public String toString() {
        return "<fn %s>".formatted(name);
    }
}
