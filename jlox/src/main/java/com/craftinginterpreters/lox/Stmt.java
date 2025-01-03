package com.craftinginterpreters.lox;

import java.util.List;

abstract class Stmt {

    interface Visitor<R> {

        R visitBlockStmt(Block stmt);

        R visitClassStmt(Class stmt);

        R visitExpressionStmt(Expression stmt);

        R visitFunctionStmt(Function stmt);

        R visitIfStmt(If stmt);

        R visitPrintStmt(Print stmt);

        R visitReturnStmt(Return stmt);

        R visitVariableStmt(Variable stmt);

        R visitWhileStmt(While stmt);

        R visitBreakStmt(Break stmt);

        R visitContinueStmt(Continue stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    static class Block extends Stmt {
        final List<Stmt> statements;

        Block(List<Stmt> statements) {
            this.statements = statements;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitBlockStmt(this);
        }
    }

    static class Class extends Stmt {
        final Token name;
        final List<Stmt.Function> methods;

        Class(Token name, List<Stmt.Function> methods) {
            this.name = name;
            this.methods = methods;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitClassStmt(this);
        }
    }

    static class Expression extends Stmt {
        final Expr expression;

        Expression(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    static class Function extends Stmt {
        final Token name;
        final List<Token> params;
        final List<Stmt> body;

        Function(Token name, List<Token> params, List<Stmt> body) {
            this.name = name;
            this.params = params;
            this.body = body;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitFunctionStmt(this);
        }
    }

    static class If extends Stmt {
        final Expr condition;
        final Stmt thenBranch;
        final Stmt elseBranch;

        If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
            this.condition = condition;
            this.thenBranch = thenBranch;
            this.elseBranch = elseBranch;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitIfStmt(this);
        }
    }

    static class Print extends Stmt {
        final Expr expression;

        Print(Expr expression) {
            this.expression = expression;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }

    static class Return extends Stmt {
        final Token keyword;
        final Expr value;

        Return(Token keyword, Expr value) {
            this.keyword = keyword;
            this.value = value;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitReturnStmt(this);
        }
    }

    static class Variable extends Stmt {
        final Token name;
        final Expr initializer;

        Variable(Token name, Expr initializer) {
            this.name = name;
            this.initializer = initializer;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitVariableStmt(this);
        }
    }

    static class While extends Stmt {
        final Expr condition;
        final Stmt body;
        final Stmt forLoopStep;

        While(Expr condition, Stmt body, Stmt forLoopStep) {
            this.condition = condition;
            this.body = body;
            this.forLoopStep = forLoopStep;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitWhileStmt(this);
        }
    }

    static class Break extends Stmt {
        final Expr loopCondition;
        final Token keyword;

        Break(Expr loopCondition, Token keyword) {
            this.loopCondition = loopCondition;
            this.keyword = keyword;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitBreakStmt(this);
        }
    }

    static class Continue extends Stmt {
        final Expr loopCondition;
        final Token keyword;

        Continue(Expr loopCondition, Token keyword) {
            this.loopCondition = loopCondition;
            this.keyword = keyword;
        }

        @Override
        <R> R  accept(Visitor<R> visitor) {
            return visitor.visitContinueStmt(this);
        }
    }
}
