package ru.compscicenter.java2017.implementor;

import java.io.IOException;

public interface ImplWriter {
    void write(Package pkg, String className, Class parentClass) throws IOException;
}
