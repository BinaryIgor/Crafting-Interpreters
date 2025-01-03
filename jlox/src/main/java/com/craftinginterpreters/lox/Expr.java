package com.craftinginterpreters.lox;

import java.util.List;

abstract class Expr {

    interface Visitor<R> {

        R visitAssignmentExpr(Assignment expr);

        R visitBinaryExpr(Binary expr);

        R visitCallExpr(Call expr);

        R visitGetExpr(Get expr);

        R visitTernaryExpr(Ternary expr);

        R visitGroupingExpr(Grouping expr);

        R visitLiteralExpr(Literal expr);

        R visitLogicalExpr(Logical expr);

        R visitSetExpr(Set expr);

        R visitThisExpr(This expr);

        R visitUnaryExpr(Unary expr);

        R visitVariableExpr(Variable expr);

        R visitFunctionExpr(Function expr);

        R visitLoxListExpr(LoxList expr);
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

    static class Call extends Expr {
        final Expr callee;
        final Token paren;
        final List<Expr> arguments;

        Call(Expr callee, Token paren, List<Expr> arguments) {
            this.callee = callee;
            this.paren = paren;
            this.arguments = arguments;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitCallExpr(this);
        }
    }

    static class Get extends Expr {
        final Expr object;
        final Token name;

        Get(Expr object, Token name) {
            this.object = object;
            this.name = name;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitGetExpr(this);
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

    static class Logical extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;

        Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    static class Set extends Expr {
        final Expr object;
        final Token name;
        final Expr value;

        Set(Expr object, Token name, Expr value) {
            this.object = object;
            this.name = name;
            this.value = value;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }
    }

    static class This extends Expr {
        final Token keyword;

        This(Token keyword) {
            this.keyword = keyword;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
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

    static class Function extends Expr {
        final List<Token> params;
        final List<Stmt> body;

        Function(List<Token> params, List<Stmt> body) {
            this.params = params;
            this.body = body;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitFunctionExpr(this);
        }
    }

    static class LoxList extends Expr {
        final List<Expr> elements;

        LoxList(List<Expr> elements) {
            this.elements = elements;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitLoxListExpr(this);
        }
    }
}
