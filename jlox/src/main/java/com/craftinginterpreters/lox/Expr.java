package com.craftinginterpreters.lox;

import java.util.List;

abstract class Expr {

    interface Visitor<R> {

        R visitAssignmentExpr(Assignment expr);

        R visitBinaryExpr(Binary expr);

        R visitTernaryExpr(Ternary expr);

        R visitGroupingExpr(Grouping expr);

        R visitLiteralExpr(Literal expr);

        R visitUnaryExpr(Unary expr);

        R visitVariableExpr(Variable expr);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Assignment extends Expr {
        final Token name;
        final Expr value;

        Assignment(Token name, Expr value) {
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitAssignmentExpr(this);
        }
    }

    static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;

        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    static class Ternary extends Expr {
        final Expr selector;
        final Expr left;
        final Expr right;
        final int selectorLine;

        Ternary(Expr selector, Expr left, Expr right, int selectorLine) {
            this.selector = selector;
            this.left = left;
            this.right = right;
            this.selectorLine = selectorLine;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitTernaryExpr(this);
        }
    }

    static class Grouping extends Expr {
        final Expr expression;

        Grouping(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

    static class Literal extends Expr {
        final Object value;

        Literal(Object value) {
            this.value = value;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    static class Unary extends Expr {
        final Token operator;
        final Expr right;

        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    static class Variable extends Expr {
        final Token name;

        Variable(Token name) {
            this.name = name;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }
}
