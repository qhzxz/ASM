package com.qh.asm;

import java.util.Arrays;
import java.util.Objects;

class Method {
    final String owner;
    final int access;
    final String name;
    final String descriptor;
    final String signature;
    final String[] exceptions;


    public Method(String owner, int access, String name, String descriptor, String signature, String[] exceptions) {
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = name + descriptor;
        this.exceptions = exceptions;
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
                Objects.equals(owner, method.owner) &&
                Objects.equals(name, method.name) &&
                Objects.equals(descriptor, method.descriptor) &&
                Objects.equals(signature, method.signature) &&
                Arrays.equals(exceptions, method.exceptions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(owner, access, name, descriptor, signature);
        result = 31 * result + Arrays.hashCode(exceptions);
        return result;
    }
}
