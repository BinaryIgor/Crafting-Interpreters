package com.craftinginterpreters.lox.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;

public class GenerateAst {

    private static final String ROOT_PACKAGE_PATH = Paths.get("", "jlox", "src", "main", "java", "com", "craftinginterpreters", "lox").toAbsolutePath().toString();

    public static void main(String[] args) throws IOException {
        var outputDir = args.length < 1 ? ROOT_PACKAGE_PATH : args[0];
        defineAst(outputDir, "Expr", List.of(
            "Assignment : Token name, Expr value",
            "Binary     : Expr left, Token operator, Expr right",
            "Ternary    : Expr selector, Expr left, Expr right, int selectorLine",
            "Grouping   : Expr expression",
            "Literal    : Object value",
            "Logical    : Expr left, Token operator, Expr right",
            "Unary      : Token operator, Expr right",
            "Variable   : Token name"
        ));
        defineAst(outputDir, "Stmt", List.of(
            "Block      : List<Stmt> statements",
            "Expression : Expr expression",
            "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
            "Print      : Expr expression",
            "Variable   : Token name, Expr initializer",
            "While      : Expr condition, Stmt body",
            "Break      : Expr loopCondition"
        ));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        var path = outputDir + "/" + baseName + ".java";
        var writer = new PrintWriter(path, StandardCharsets.UTF_8);

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");
        // Visitor
        writer.println();
        defineVisitor(writer, baseName, types);
        // the base accept() method
        writer.println();
        writer.println("    abstract <R> R accept(Visitor<R> visitor);");
        // The AST classes
        for (var t : types) {
            var classNameFields = t.split(":");
            var className = classNameFields[0].trim();
            var fields = classNameFields[1].trim();
            writer.println();
            defineType(writer, baseName, className, fields);
        }
        writer.println("}");
        writer.close();
    }

    private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
        writer.println("    static class %s extends %s {".formatted(className, baseName));
        // Fields
        var fields = fieldList.split(", ");
        for (var f : fields) {
            writer.println("        final %s;".formatted(f));
        }
        writer.println();
        // Constructor
        writer.println("        %s(%s) {".formatted(className, fieldList));
        // Store parameters in the fields
        for (var f : fields) {
            var name = f.split(" ")[1];
            writer.println("            this.%s = %s;".formatted(name, name));
        }
        writer.println("        }");
        // Visitor pattern
        writer.println();
        writer.println("        @Override");
        writer.println("        <R> R  accept(Visitor<R> visitor) {");
        writer.println("            return visitor.visit%s%s(this);".formatted(className, baseName));
        writer.println("        }");

        writer.println("    }");
    }

    private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
        writer.println("    interface Visitor<R> {");
        for (var type : types) {
            var typeName = type.split(":")[0].trim();
            writer.println();
            writer.println("        R visit%s%s(%s %s);".formatted(typeName, baseName, typeName, baseName.toLowerCase()));
        }
        writer.println("    }");
    }
}
