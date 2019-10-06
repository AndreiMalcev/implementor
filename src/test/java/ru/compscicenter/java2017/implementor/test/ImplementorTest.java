package ru.compscicenter.java2017.implementor.test;

import org.fest.assertions.api.BooleanAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.compscicenter.java2017.implementor.Implementor;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.*;

import static org.fest.assertions.api.Assertions.assertThat;

public class ImplementorTest {

    static final private String TESTS_DIRECTORY = "target/tmp/resources";
    static final private String OUTPUT_DIRECTORY = "target/tmp/java";

    private Class<?> implementorClass;

    @Before
    public void getInstance() throws ClassNotFoundException, IOException, IllegalAccessException, InstantiationException {
        Properties prop = new Properties();
        prop.load(ImplementorTest.class.getClassLoader().getResourceAsStream("build.properties"));
        Locale.setDefault(Locale.US);
        implementorClass = Class.forName(prop.getProperty("IMPLEMENTATION_CLASS"));
    }

    @Test
    public void shouldBeConstructorWithStringParameter() throws Exception {
        newImplementor();
    }

    @Test
    public void implementCloneable() throws Exception {
        checkInterfaceImplementationFromStandardLibrary("java.lang.Cloneable");
    }

    private void deleteFolderContent(File folder, boolean isInner) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty dirs
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteFolderContent(f, true);
                } else {
                    f.delete();
                }
            }
        }
        if (isInner) {
            folder.delete();
        }
    }

    @After
    public void cleanUp() {
        deleteFolderContent(new File(OUTPUT_DIRECTORY), true);
    }


    public void checkInterfaceImplementationFromFolder(String className) throws Exception {
        Implementor implementor = newImplementor();
        String implClassName = implementor.implementFromDirectory(TESTS_DIRECTORY, className);
        compileAndCheckInterfaceImplementation(className, implClassName);
    }

    public void checkInterfaceImplementationFromStandardLibrary(String className) throws Exception {
        Implementor implementor = newImplementor();
        String implClassName = implementor.implementFromStandardLibrary(className);
        compileAndCheckInterfaceImplementation(className, implClassName);
    }

    public void checkAbstractClassImplementationFromFolder(String className) throws Exception {
        Implementor implementor = newImplementor();
        String implClassName = implementor.implementFromDirectory(TESTS_DIRECTORY, className);
        compileAndCheckAbstractClassImplementation(className, implClassName);
    }

    public void checkAbstractClassImplementationFromStandardLibrary(String className) throws Exception {
        Implementor implementor = newImplementor();
        String implClassName = implementor.implementFromStandardLibrary(className);
        compileAndCheckAbstractClassImplementation(className, implClassName);
    }


    public void compileAndCheckInterfaceImplementation(String className, String implClassName) throws IOException {
        final Class<?> outputClass = compileAndLoadClass(implClassName);
        checkImplementsInterface(className, outputClass);
    }

    public void compileAndCheckAbstractClassImplementation(String className, String implClassName) throws IOException {
        final Class<?> outputClass = compileAndLoadClass(implClassName);
        checkExtendsAbstractClass(className, outputClass);
    }

    public void checkExtendsAbstractClass(String className, Class<?> outputClass) {
        assertThat(outputClass.getSuperclass().getCanonicalName()).isEqualTo(className);
    }


    private Class<?> compileAndLoadClass(String implClassName) throws IOException {
        final String outputAbsolutePath = getAbsolutePath(implClassName);
        tryToCompile(outputAbsolutePath);
        final Class<?> outputClass = loadClassFromTestDirectory(implClassName);
        checkIsNotAbstract(outputClass);
        return outputClass;
    }


    public void checkImplementsInterface(String className, Class<?> aClass) {
        assertThat(aClass.getInterfaces()).hasSize(1);
        assertThat(aClass.getInterfaces()[0].getCanonicalName()).isEqualTo(className);
    }

    public BooleanAssert checkIsNotAbstract(Class<?> aClass) {
        return assertThat(Modifier.isAbstract(aClass.getModifiers())).isFalse();
    }

    public void tryToCompile(String outputAbsolutePath) throws IOException {
        assertThat(compileFile(outputAbsolutePath)).isTrue().as("Can't compile " + outputAbsolutePath);
    }

    private String getAbsolutePath(String implClassName) {
        final String[] split = implClassName.split("\\.");
        split[split.length - 1] += ".java";
        return Paths.get(OUTPUT_DIRECTORY, split).toAbsolutePath().toString();
    }

    private boolean compileFile(String absolutePath) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromStrings(
                Arrays.asList(absolutePath));
        List<String> options = new ArrayList<>();
        options.addAll(Arrays.asList("-classpath", System.getProperty("java.class.path") + TESTS_DIRECTORY));
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options,
                null, compilationUnits);
        boolean success = task.call();
        if (!success) {
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                System.err.println(diagnostic.toString());
            }
        }
        fileManager.close();
        return success;
    }

    private Class<?> loadClassFromTestDirectory(String className) {
        File outputDirectoryFile = new File(OUTPUT_DIRECTORY);
        File testDirectoryFile = new File(TESTS_DIRECTORY);

        try {
            // Convert File to a URL
            URL[] urls = new URL[]{outputDirectoryFile.toURI().toURL(),
                    testDirectoryFile.toURI().toURL()};

            // Create a new class loader with the directory
            ClassLoader cl = new URLClassLoader(urls);

            // Load in the class; MyClass.class should be located in
            // the directory file:/c:/myclasses/com/mycompany
            return cl.loadClass(className);
        } catch (MalformedURLException | ClassNotFoundException ignored) {
            throw new RuntimeException("Class cannot be loaded");
        }
    }

    /*
     * This is constructor without parameters for your Implementor implementation.
     */
    private Implementor newImplementor() throws Exception {
        Constructor<?> constructor = getNoArgConstructor();
        constructor.setAccessible(true);
        return (Implementor) constructor.newInstance(OUTPUT_DIRECTORY);
    }


    private Constructor<?> getNoArgConstructor() throws Exception {
        return implementorClass.getDeclaredConstructor(String.class);
    }

}
