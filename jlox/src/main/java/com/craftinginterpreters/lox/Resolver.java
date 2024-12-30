package com.craftinginterpreters.lox;

import java.util.*;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;
    private boolean currentlyInLoop = false;

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visitAssignmentExpr(Expr.Assignment expr) {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr) {
        resolve(expr.callee);
        expr.arguments.forEach(this::resolve);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr) {
        resolve(expr.selector);
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr) {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr) {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr) {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr) {
        if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme()) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.");
        }
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitFunctionExpr(Expr.Function expr) {
        resolveFunction(expr.params, expr.body, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt.params, stmt.body, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        Optional.ofNullable(stmt.elseBranch)
            .ifPresent(this::resolve);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code");
        }
        Optional.ofNullable(stmt.value).ifPresent(this::resolve);
        return null;
    }

    @Override
    public Void visitVariableStmt(Stmt.Variable stmt) {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        var previouslyInLoop = currentlyInLoop;
        try {
            currentlyInLoop = true;
            resolve(stmt.condition);
            resolve(stmt.body);
            Optional.ofNullable(stmt.forLoopStep).ifPresent(this::resolve);
        } finally {
            currentlyInLoop = previouslyInLoop;
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        if (!currentlyInLoop) {
            Lox.error(stmt.keyword, "Can't break from top-level code");
        }
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt) {
        if (!currentlyInLoop) {
            Lox.error(stmt.keyword, "Can't continue from top-level code");
        }
        return null;
    }

    void resolve(List<Stmt> statements) {
        statements.forEach(this::resolve);
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) {
            return;
        }
        var scope = scopes.peek();
        if (scope.containsKey(name.lexeme())) {
            Lox.error(name, "Already a variable with this name in this scope");
        }
        scope.put(name.lexeme(), false);
    }

    private void define(Token name) {
        if (!scopes.isEmpty()) {
            scopes.peek().put(name.lexeme(), true);
        }
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme())) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    private void resolveFunction(List<Token> params, List<Stmt> body, FunctionType type) {
        var enclosingFunction = currentFunction;
        currentFunction = type;
        try {
            beginScope();
            params.forEach(p -> {
                declare(p);
                define(p);
            });
            resolve(body);
            endScope();
        } finally {
            currentFunction = enclosingFunction;
        }
    }
}
