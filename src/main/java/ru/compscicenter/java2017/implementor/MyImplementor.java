package ru.compscicenter.java2017.implementor;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public final class MyImplementor implements Implementor {

    private String outputDirectory;
    private StringBuilder inClass;
    private Class parentClass;
    private String className;

    public MyImplementor(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        inClass = new StringBuilder();
    }

    @Override
    public String implementFromDirectory(String directoryPath, String className) throws ImplementorException {
        Path path = Paths.get(directoryPath).toAbsolutePath();
        URL url = null;
        try {
            url = path.toUri().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        ClassLoader classLoader = new URLClassLoader(new URL[]{url});
        try {
            parentClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ImplementorException("Class not found", e);
        }
        return newClass(parentClass.getPackage());
    }

    @Override
    public String implementFromStandardLibrary(String className) throws ImplementorException {
        try {
            parentClass = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ImplementorException("Class not found", e);
        }
        return newClass(null);
    }

    private String newClass(Package aPackage) throws ImplementorException {
        if (Modifier.isFinal(parentClass.getModifiers())) {
            throw new ImplementorException("Final class");
        }
        if (!parentClass.isInterface()) {
            boolean openConstructor = false;
            for (Constructor cons : parentClass.getDeclaredConstructors()) {
                if (!Modifier.isPrivate(cons.getModifiers())) {
                    openConstructor = true;
                }
            }
            if (!openConstructor) {
                throw new ImplementorException("Private constructors");
            }
        }
        String newClassName = parentClass.getSimpleName() + "Impl";
        String newFileName = newClassName + ".java";
        String newDirectory = "";
        if (aPackage != null) {
            newDirectory = aPackage.getName().replaceAll("\\.", "/");
        }
        File outputFile = Paths.get(outputDirectory, newDirectory, newFileName).toFile();
        try {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        } catch (IOException e) {
            throw new ImplementorException("", e);
        }
        try (FileWriter fileWriter = new FileWriter(outputFile)) {
            className = newClassName;
            print(aPackage);
            fileWriter.append(inClass);
        } catch (IOException e) {
            throw new ImplementorException("", e);
        }
        if (aPackage != null) {
            return aPackage.getName() + "." + newClassName;
        } else {
            return newClassName;
        }
    }

    private void print(Package aPackage) throws IOException {
        if (aPackage != null) {
            printPackage(aPackage);
            inClass.append("\n");
        }
        printClass();
    }

    private void printPackage(Package aPackage) throws IOException {
        String formatPackage = String.format("package %s;\n", aPackage.getName());
        inClass.append(formatPackage);
    }

    private void printClass() throws IOException {
        printNameClass();
        boolean noConctructor = parentClass.isInterface();
        Constructor constructor = null;
        for (Constructor cons : parentClass.getDeclaredConstructors()) {
            if (!Modifier.isPrivate(cons.getModifiers())) {
                constructor = cons;
                if (cons.getParameterTypes().length == 0) {
                    noConctructor = true;
                }
            }
        }
        if (!noConctructor) {
            inClass.append("\n");
            printParamConstructor(className, constructor);
        }
        for (Method method : getAllMethods(parentClass)) {
            if (Modifier.isAbstract(method.getModifiers())) {
                inClass.append("\n");
                printMethodName(method);
                printMethod(method);
                inClass.append("}\n");
            }
        }
        inClass.append("}\n");
    }

    private void printParamConstructor(String className, Constructor constructor) throws IOException {
        inClass.append(" ");
        StringBuilder constructorLine = new StringBuilder();
        constructorLine.append(Modifier.toString(constructor.getModifiers())).append(" ");
        constructorLine.append(className).append("()");
        Class[] exceptionTypes = constructor.getExceptionTypes();
        constructorLine.append(exceptions(exceptionTypes));
        inClass.append(constructorLine.toString()).append(" {\n").append(" ");
        inClass.append("super(");
        Class[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i].equals(Character.TYPE)) {
                inClass.append("\\0");
            } else if (parameterTypes[i].equals(Boolean.TYPE)) {
                inClass.append("false");
            } else if (parameterTypes[i].isPrimitive()) {
                inClass.append("0");
            } else {
                inClass.append("null");
            }
            if (i != parameterTypes.length - 1) {
                inClass.append(", ");
            }
        }

        inClass.append(");").append("\n").append("}\n");
    }


    private StringBuilder exceptions(Class[] exceptions) {
        StringBuilder stringBuilder = new StringBuilder();
        if (exceptions.length > 0) {
            stringBuilder.append(" throws ");
            for (int i = 0; i < exceptions.length; i++) {
                stringBuilder.append(exceptions[i].getCanonicalName());
                if (i != exceptions.length - 1) {
                    stringBuilder.append(", ");
                }
            }
        }
        return stringBuilder;
    }


    private List<Method> getAllMethods(Class clazz) {
        List<Method> allMethod = new ArrayList<>(Arrays.asList(clazz.getDeclaredMethods()));
        Set<List<String>> setMethods = new HashSet<>();
        for (Method method : allMethod) {
            List<String> stringMethods = new ArrayList<>();
            stringMethods.add(method.getName());
            for (Class t : method.getParameterTypes()) {
                stringMethods.add(t.getCanonicalName());
            }
            setMethods.add(stringMethods);
        }
        for (Class interf : clazz.getInterfaces()) {
            for (Method method : getAllMethods(interf)) {
                List<String> stringMethods = new ArrayList<>();
                stringMethods.add(method.getName());
                for (Class t : method.getParameterTypes()) {
                    stringMethods.add(t.getCanonicalName());
                }
                if (!setMethods.contains(stringMethods)) {
                    setMethods.add(stringMethods);
                    allMethod.add(method);
                }
            }
        }
        if (clazz.getSuperclass() != null) {
            for (Method method : getAllMethods(clazz.getSuperclass())) {
                List<String> name = new ArrayList<>();
                name.add(method.getName());
                for (Class t : method.getParameterTypes()) {
                    name.add(t.getCanonicalName());
                }
                if (!setMethods.contains(name)) {
                    setMethods.add(name);
                    allMethod.add(method);
                }
            }
        }
        return allMethod;
    }

    private void printNameClass() throws IOException {
        inClass.append(" ");
        String word;
        if (parentClass.isInterface()) {
            word = "implements";
        } else {
            word = "extends";
        }
        String s = String.format("public class %s %s %s", className, word, parentClass.getCanonicalName());
        inClass.append(s).append(" {\n");
    }

    private void printMethodName(Method method) throws IOException {
        inClass.append(" ");
        int type = Modifier.FINAL | Modifier.STATIC | Modifier.PUBLIC | Modifier.PRIVATE | Modifier.PROTECTED;
        int modifiers = method.getModifiers() & type;
        StringBuilder methodLine = new StringBuilder();
        methodLine.append(Modifier.toString(modifiers)).append(" ");
        methodLine.append(method.getReturnType().getCanonicalName());
        methodLine.append(" ").append(method.getName()).append("(");
        Parameter[] param = method.getParameters();
        for (int i = 0; i < param.length; i++) {
            methodLine.append(param[i].getType()
                    .getCanonicalName()).append(" ")
                    .append(param[i].getName());
            if (i != param.length - 1) {
                methodLine.append(", ");
            }
        }
        methodLine.append(")");
        Class[] exceptionTypes = method.getExceptionTypes();
        methodLine.append(exceptions(exceptionTypes));
        inClass.append(methodLine.toString()).append(" {\n");
    }

    private void printMethod(Method method) throws IOException {
        if (method.getReturnType().equals(Void.TYPE)) {
            return;
        }
        inClass.append(" ");
        if (method.getReturnType().equals(Character.TYPE)) {
            inClass.append("return '\\0';");
        } else if (method.getReturnType().equals(Boolean.TYPE)) {
            inClass.append("return false;");
        } else if (method.getReturnType().isPrimitive()) {
            inClass.append("return 0;");
        } else {
            inClass.append("return null;");
        }
        inClass.append("\n");
    }
}