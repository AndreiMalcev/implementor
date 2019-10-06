package ru.compscicenter.java2017.implementor;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class CSCImplWriter implements ImplWriter {

    private static final int INDENT_WIDTH = 4;

    private Writer writer;

    public CSCImplWriter(Writer writer) {
        this.writer = writer;
    }

    public void write(Package pkg, String className, Class parentClass) throws IOException {
        if (pkg != null) {
            writePackageDirective(pkg);
            writeNewline();
        }
        writeClass(className, parentClass);
    }

    private void writePackageDirective(Package pkg) throws IOException {
        String packageDirective = String.format("package %s;\n", pkg.getName());
        writer.write(packageDirective);
    }

    private void writeClass(String className, Class parentClass) throws IOException {
        writeClass(className, parentClass, 0);
    }

    private void writeClass(String className, Class parentClass, int indentLevel) throws IOException {
        writeClassDeclaration(className, parentClass, indentLevel);

        boolean hasDefaultConstructor = parentClass.isInterface();
        Constructor goodConstructor = null;
        for (Constructor c : parentClass.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(c.getModifiers())) {
                goodConstructor = c;
                if (c.getParameterTypes().length == 0) {
                    hasDefaultConstructor = true;
                }
            }
        }

        if (!hasDefaultConstructor) {
            writeNewline();
            writeParameterlessConstructor(className, goodConstructor, indentLevel + 1);
        }

        for (Method method : getAllMethods(parentClass)) {
            if (Modifier.isAbstract(method.getModifiers())) {
                writeNewline();

                writeMethodDeclaration(method, indentLevel + 1);
                writeMethodStub(method, indentLevel + 2);

                writeClosingBrace(indentLevel + 1);
            }
        }

        writeClosingBrace(indentLevel);
    }

    private void writeParameterlessConstructor(String className, Constructor goodConstructor, int indentLevel)
            throws IOException {
        writeIndent(indentLevel);

        StringBuilder constructorDeclaration = new StringBuilder();

        int modifiers = goodConstructor.getModifiers();
        modifiers &= Modifier.FINAL | Modifier.STATIC | Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;
        constructorDeclaration.append(Modifier.toString(modifiers));
        constructorDeclaration.append(" ");
        constructorDeclaration.append(className);
        constructorDeclaration.append("()");

        Class[] exceptions = goodConstructor.getExceptionTypes();
        if (exceptions.length > 0) {
            constructorDeclaration.append(" throws ");
            for (int i = 0; i < exceptions.length; i++) {
                constructorDeclaration.append(exceptions[i].getCanonicalName());
                if (i != exceptions.length - 1) {
                    constructorDeclaration.append(", ");
                }
            }
        }

        writer.write(constructorDeclaration.toString());

        writeOpeningBrace();
        writeIndent(indentLevel + 1);
        writer.write("super(");

        Class[] parameterTypes = goodConstructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].equals(Character.TYPE)) {
                writer.write("\\0");
            } else if (parameterTypes[i].equals(Boolean.TYPE)) {
                writer.write("false");
            } else if (parameterTypes[i].isPrimitive()) {
                writer.write("0");
            } else {
                writer.write("null");
            }
            if (i != parameterTypes.length - 1) {
                writer.write(", ");
            }
        }

        writer.write(");");
        writeNewline();
        writeClosingBrace(indentLevel);
    }

    private void writeClassDeclaration(String className, Class parentClass, int indentLevel) throws IOException {
        writeIndent(indentLevel);

        String keyword = parentClass.isInterface() ? "implements" : "extends";
        String classDeclaration = String.format("public class %s %s %s", className, keyword,
                parentClass.getCanonicalName());
        writer.write(classDeclaration);

        writeOpeningBrace();
    }

    private void writeMethodDeclaration(Method method, int indentLevel) throws IOException {
        writeIndent(indentLevel);

        int modifiers = method.getModifiers();
        modifiers &= Modifier.FINAL | Modifier.STATIC | Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;

        StringBuilder methodDeclaration = new StringBuilder();

        // Listing all modifiers of super class (excluding abstract)
        methodDeclaration.append(Modifier.toString(modifiers));
        methodDeclaration.append(" ");

        methodDeclaration.append(method.getReturnType().getCanonicalName());
        methodDeclaration.append(" ");
        methodDeclaration.append(method.getName());

        methodDeclaration.append("(");
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            methodDeclaration.append(params[i].getType().getCanonicalName());
            methodDeclaration.append(" ");
            methodDeclaration.append(params[i].getName());
            if (i != params.length - 1) {
                methodDeclaration.append(", ");
            }
        }
        methodDeclaration.append(")");

        Class[] exceptions = method.getExceptionTypes();
        if (exceptions.length > 0) {
            methodDeclaration.append(" throws ");
            for (int i = 0; i < exceptions.length; i++) {
                methodDeclaration.append(exceptions[i].getCanonicalName());
                if (i != exceptions.length - 1) {
                    methodDeclaration.append(", ");
                }
            }
        }

        writer.write(methodDeclaration.toString());
        writeOpeningBrace();
    }

    private void writeMethodStub(Method method, int indentLevel) throws IOException {
        if (method.getReturnType().equals(Void.TYPE)) {
            return; // Do nothing
        }

        writeIndent(indentLevel);
        if (method.getReturnType().equals(Character.TYPE)) {
            writer.write("return '\\0';");
        } else if (method.getReturnType().equals(Boolean.TYPE)) {
            writer.write("return false;");
        } else if (method.getReturnType().isPrimitive()) {
            // Remaining primitives are numeric
            writer.write("return 0;");
        } else {
            writer.write("return null;");
        }
        writeNewline();
    }

    private void writeNewline() throws IOException {
        writer.write("\n");
    }

    private void writeOpeningBrace() throws IOException {
        writer.write(" {\n");
    }

    private void writeClosingBrace(int indentLevel) throws IOException {
        writeIndent(indentLevel);
        writer.write("}\n");
    }

    private void writeIndent(int indentLevel) throws IOException {
        for (int i = 0; i < indentLevel * INDENT_WIDTH; i++) {
            writer.write(' ');
        }
    }

    private static List<Method> getAllMethods(Class cls) {
        List<Method> methods = new ArrayList<>(Arrays.asList(cls.getDeclaredMethods()));
        Set<List<String>> signatures = new HashSet<>();

        for (Method method : methods) {
            List<String> signature = new ArrayList<>();
            signature.add(method.getName());
            for (Class t : method.getParameterTypes()) {
                signature.add(t.getCanonicalName());
            }
            signatures.add(signature);
        }

        for (Class inter : cls.getInterfaces()) {
            for (Method method : getAllMethods(inter)) {
                List<String> signature = new ArrayList<>();
                signature.add(method.getName());
                for (Class t : method.getParameterTypes()) {
                    signature.add(t.getCanonicalName());
                }
                if (!signatures.contains(signature)) {
                    signatures.add(signature);
                    methods.add(method);
                }
            }
        }

        if (cls.getSuperclass() != null) {
            for (Method method : getAllMethods(cls.getSuperclass())) {
                List<String> signature = new ArrayList<>();
                signature.add(method.getName());
                for (Class t : method.getParameterTypes()) {
                    signature.add(t.getCanonicalName());
                }
                if (!signatures.contains(signature)) {
                    signatures.add(signature);
                    methods.add(method);
                }
            }
        }

        return methods;
    }
}
