package ru.compscicenter.java2017.implementor;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;

public final class CSCImplementor implements Implementor {

    private String outputDirectory;

    public CSCImplementor(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @Override
    public String implementFromDirectory(String directoryPath, String className) throws ImplementorException, MalformedURLException {
        Path path = Paths.get(directoryPath).toAbsolutePath();
        URL localURL;
            localURL = path.toUri().toURL();
        ClassLoader classLoader = new URLClassLoader(new URL[] {localURL});
        Class clazz;
        try {
            clazz = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new ImplementorException("Class not found", e);
        }
        return implement(clazz.getPackage(), clazz, this.outputDirectory);
    }

    @Override
    public String implementFromStandardLibrary(String className) throws ImplementorException {
        Class cls;
        try {
            cls = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new ImplementorException("Class not found", e);
        }
        return implement(null, cls, this.outputDirectory);
    }

    private String implement(Package pkg, Class cls, String outDir) throws ImplementorException {
        if (Modifier.isFinal(cls.getModifiers())) {
            throw new ImplementorException("Final class");
        }

        if (!cls.isInterface()) {
            boolean hasGoodConstructor = false;
            for (Constructor c : cls.getDeclaredConstructors()) {
                if (!Modifier.isPrivate(c.getModifiers()))
                    hasGoodConstructor = true;
            }
            if (!hasGoodConstructor) {
                throw new ImplementorException("Private constructors");
            }
        }

        String implName = cls.getSimpleName() + "Impl";
        String implFilename = implName + ".java";
        String packageDir = "";
        if (pkg != null) {
            packageDir = pkg.getName().replaceAll("\\.", "/");
        }
        File outputFile = Paths.get(outDir, packageDir, implFilename).toFile();
        try {
            outputFile.getParentFile().mkdirs();
            outputFile.createNewFile();
        } catch (IOException e) {
            throw new ImplementorException("", e);
        }
        try (FileWriter fileWriter = new FileWriter(outputFile)) {
            ImplWriter implWriter = new CSCImplWriter(fileWriter);
            implWriter.write(pkg, implName, cls);
        } catch (IOException e) {
            throw new ImplementorException("", e);
        }
        if (pkg != null) {
            return pkg.getName() + "." + implName;
        } else {
            return implName;
        }
    }
}
