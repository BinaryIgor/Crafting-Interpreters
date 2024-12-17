package com.craftinginterpreters.lox;

import java.util.function.BiFunction;

public class Interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expression) {
        try {
            var value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
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
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");
            }
            case SLASH -> ensureNumberOperands(expr.operator, left, right, (l, r) -> l / r);
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
        throw new IllegalStateException("Not supported yet!");
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
