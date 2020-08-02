package com.qh.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class AccessMethodPicker {
    private final Map<String, ClassContext> mMap;

    AccessMethodPicker(Map<String, ClassContext> mMap) {
        this.mMap = mMap;
    }

    void pick() {

    }


    private class AccessMethodClassPicker extends ClassVisitor {
        private String className;

        public AccessMethodClassPicker(ClassVisitor classVisitor) {
            super(Opcodes.ASM6, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            className = name;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            ClassContext classContext = mMap.get(className);
            if (classContext.targetSignatureSet.contains(name + descriptor)) {
                access = Utils.removePrivateAccessFlag(access);
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            ClassContext classContext = mMap.get(className);
            if (classContext.targetSignatureSet.contains(name + descriptor)) {
                access = Utils.removePrivateAccessFlag(access);
            }
            return new Picker(super.visitMethod(access, name, descriptor, signature, exceptions));
        }

    }

    private class Picker extends MethodVisitor {

        public Picker(MethodVisitor methodVisitor) {
            super(Opcodes.ASM6, methodVisitor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            ClassContext classContext = mMap.get(owner);
            String signature = name + descriptor;
            if (classContext != null && classContext.accessMethodMap.containsKey(signature) && !isInterface) {
                AccessMethod accessMethod = classContext.accessMethodMap.get(signature);
                AccessType type = accessMethod.type;

                if (type == AccessType.NONE) {
                    throw new IllegalStateException("method:" + accessMethod.accessMethod.toString() + " access type unknown");
                } else if (type == AccessType.INVOKE) {
                    Method targetMethod = (Method) accessMethod.target;
                    boolean isStatic = Utils.isContainStaticAccessFlag(targetMethod.access);
                    mv.visitMethodInsn(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, owner, targetMethod.name, targetMethod.descriptor);
                } else {
                    Field field = (Field) accessMethod.target;
                    if (type == AccessType.GET) {
                        boolean isStatic = Utils.isContainStaticAccessFlag(field.access);
                        mv.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner, field.name, field.descriptor);
                    } else if (type == AccessType.SET) {

                    }
                }

            } else {
                if (classContext != null
                        && classContext.targetSignatureSet.contains(signature)
                        && opcode == Opcodes.INVOKESPECIAL
                        && !isInterface) {
                    opcode = Opcodes.INVOKEVIRTUAL;
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        }
    }
}

