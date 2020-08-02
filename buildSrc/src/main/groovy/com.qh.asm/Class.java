package com.qh.asm;

import java.util.Arrays;
import java.util.Objects;

public class Class {
    int access;
    String name;
    String superName;
    String interfaces[];

    public Class(int access, String name, String superName, String[] interfaces) {
        this.access = access;
        this.name = name;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public String getPackage() {
        int aIndex = name.lastIndexOf("/");
        if (aIndex == -1) return "";
        return name.substring(0, aIndex);
    }


    public String getSimpleName() {
        int aIndex = name.lastIndexOf("/");
        if (aIndex == -1) return "";
        return name.substring(aIndex + 1);
    }

    @Override
    public String toString() {
        return "Class{" +
                "access=" + access +
                ", name='" + name + '\'' +
                ", superName='" + superName + '\'' +
                ", interfaces=" + Arrays.toString(interfaces) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Class aClass = (Class) o;
        return access == aClass.access &&
                Objects.equals(name, aClass.name) &&
                Objects.equals(superName, aClass.superName) &&
                Arrays.equals(interfaces, aClass.interfaces);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(access, name, superName);
        result = 31 * result + Arrays.hashCode(interfaces);
        return result;
    }
}
