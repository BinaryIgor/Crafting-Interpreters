package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.List;

public class LoxList {

    private final List<Object> elements;

    LoxList(List<Object> elements) {
        this.elements = new ArrayList<>(elements);
    }

    Object get(int idx) {
        return elements.get(idx);
    }

    Object set(int idx, Object element) {
        return elements.set(idx, element);
    }

    void add(Object element) {
        elements.add(element);
    }

    void delete(int idx) {
        elements.remove(idx);
    }

    int size() {
        return elements.size();
    }

    @Override
    public String toString() {
        return elements.toString();
    }

    static void defineFunctions(Environment globals) {
        globals.define("get", new LoxCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                // TODO: validate types!
                var list = (LoxList) arguments.getFirst();
                var idx = (Number) arguments.getLast();
                return list.get(idx.intValue());
            }
        });

        globals.define("set", new LoxCallable() {
            @Override
            public int arity() {
                return 3;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                var listIdx = ensureListAndIdx(arguments);
                var element = arguments.getLast();
                return listIdx.list.set(listIdx.number, element);
            }
        });

        globals.define("add", new LoxCallable() {
            @Override
            public int arity() {
                return 2;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                ensureList(arguments).add(arguments.getLast());
                return null;
            }
        });

        globals.define("size", new LoxCallable() {
            @Override
            public int arity() {
                return 1;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) ensureList(arguments).size();
            }
        });
    }

    private static ListAndNumber ensureListAndIdx(List<Object> arguments) {
        // TODO: validate types!
        var list = ensureList(arguments);
        var idx = (Number) arguments.get(1);
        return new ListAndNumber(list, idx.intValue());
    }

    private static LoxList ensureList(List<Object> arguments) {
        return (LoxList) arguments.getFirst();
    }

    private record ListAndNumber(LoxList list, int number) {
    }
}
