package com.qh.asm;

import java.util.Arrays;
import java.util.Objects;

class Method {
    final int access;
    final String name;
    final String descriptor;
    final String signature;
    final String[] exceptions;

    public Method(int access, String name, String descriptor, String signature, String[] exceptions) {
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = name + descriptor;
        this.exceptions = exceptions;
    }

    @Override
    public String toString() {
        return "Method{" +
                "access=" + access +
                ", name='" + name + '\'' +
                ", descriptor='" + descriptor + '\'' +
                ", signature='" + signature + '\'' +
                ", exceptions=" + Arrays.toString(exceptions) +
                '}';
    }

    public String getReturnType() {
        if (Utils.isEmpty(descriptor)) return "";
        int index = descriptor.indexOf(")");
        if (index == -1) return "";
        return descriptor.substring(index + 1);
    }

    public String getSignature() {
        return signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Method method = (Method) o;
        return access == method.access &&
                Objects.equals(name, method.name) &&
                Objects.equals(descriptor, method.descriptor) &&
                Objects.equals(signature, method.signature) &&
                Arrays.equals(exceptions, method.exceptions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(access, name, descriptor, signature);
        result = 31 * result + Arrays.hashCode(exceptions);
        return result;
    }
}
