package com.craftinginterpreters.lox;

import java.util.List;
import java.util.function.BiFunction;

public class Interpreter implements Stmt.Visitor<Void>, Expr.Visitor<Object> {

    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            statements.forEach(this::execute);
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitVariableStmt(Stmt.Variable stmt) {
        var value = stmt.initializer == null ? null : evaluate(stmt.initializer);
        environment.define(stmt.name.lexeme(), value);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        var value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        var previous = this.environment;
        try {
            this.environment = environment;
            statements.forEach(this::execute);
        } finally {
            this.environment = previous;
        }
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }
        if (object instanceof Double) {
            var text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Object visitAssignmentExpr(Expr.Assignment expr) {
        var value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        var left = evaluate(expr.left);
        var right = evaluate(expr.right);

        return switch (expr.operator.type()) {
            case MINUS -> ensureNumberOperands(expr.operator, left, right, (l, r) -> l - r);
            case PLUS -> {
                if (left instanceof Double dLeft && right instanceof Double dRight) {
                    yield dLeft + dRight;
                }
                if (left instanceof String sLeft && right instanceof String sRight) {
                    yield sLeft + sRight;
                }
                if (left instanceof String sLeft) {
                    yield sLeft + stringify(right);
                }
                if (right instanceof String sRight) {
                    yield stringify(left) + sRight;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or at least one string");
            }
            case SLASH -> ensureNumberOperands(expr.operator, left, right, (l, r) -> {
                if (r == 0) {
                    throw new RuntimeError(expr.operator, "Division by zero");
                }
                return l / r;
            });
            case STAR -> ensureNumberOperands(expr.operator, left, right, (l, r) -> l * r);
            case GREATER -> ensureNumberOperands(expr.operator, left, right, (l, r) -> l > r);
            case GREATER_EQUAL -> ensureNumberOperands(expr.operator, left, right, (l, r) -> l >= r);
            case LESS -> ensureNumberOperands(expr.operator, left, right, (l, r) -> l < r);
            case LESS_EQUAL -> ensureNumberOperands(expr.operator, left, right, (l, r) -> l <= r);
            case BANG_EQUAL -> !isEqual(left, right);
            case EQUAL_EQUAL -> isEqual(left, right);
            default -> throw notSupportedOperatorException(expr.operator, "Binary");
        };
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null) {
            return false;
        }
        return a.equals(b);
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        var selector = evaluate(expr.selector);

        if (!(selector instanceof Boolean selectorValue)) {
            throw new RuntimeError(expr.selectorLine, "Ternary selector must evaluate to boolean value but was: " + selector);
        }

        return selectorValue ? evaluate(expr.left) : evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        var right = evaluate(expr.right);
        return switch (expr.operator.type()) {
            case MINUS -> {
                checkNumberOperand(expr.operator, right);
                yield -(double) right;
            }
            case BANG -> isTruthy(right);
            default -> throw notSupportedOperatorException(expr.operator, "Unary");
        };
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (!(operand instanceof Double)) {
            throw new RuntimeError(operator, "Operand must be a number");
        }
    }

    private Object ensureNumberOperands(Token operator, Object left, Object right,
                                        BiFunction<Double, Double, Object> func) {
        if (left instanceof Double dLeft && right instanceof Double dRight) {
            return func.apply(dLeft, dRight);
        }
        throw new RuntimeError(operator, "Operands must be numbers");
    }

    private RuntimeError notSupportedOperatorException(Token operator, String expression) {
        return new RuntimeError(operator, "Not supported operator in %s expression".formatted(expression));
    }

    private boolean isTruthy(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return true;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
}
