package com.qh.asm;

import java.util.Objects;

class Field {
    final String owner;
    final int access;
    final String name;
    final String descriptor;
    final String signature;
    final Object value;

    Field(String owner, int access, String name, String descriptor, String signature, Object value) {
        this.owner = owner;
        this.access = access;
        this.name = name;
        this.descriptor = descriptor;
        this.signature = name + descriptor;
        this.value = value;
    }

    public String getSignature() {
        return signature;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Field field = (Field) o;
        return access == field.access &&
                Objects.equals(owner, field.owner) &&
                Objects.equals(name, field.name) &&
                Objects.equals(descriptor, field.descriptor) &&
                Objects.equals(signature, field.signature) &&
                Objects.equals(value, field.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, access, name, descriptor, signature, value);
    }
}
