package com.qh.asm;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RemoveAccessClassVisitor extends ClassVisitor {
    private String className;
    private Transaction transaction;

    public RemoveAccessClassVisitor(Transaction ctx, ClassVisitor classVisitor, String clzName) {
        super(Opcodes.ASM5, classVisitor);
        transaction = ctx;
        className = clzName;
    }


    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        Transaction.ClassContext clzCtx = transaction.getClzCtx(className);
        if (clzCtx != null && clzCtx.needExtend(name + descriptor)) {
            access = Utils.removePrivateAccessFlag(access);
        }
        return super.visitField(access, name, descriptor, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        Transaction.ClassContext clzCtx = transaction.getClzCtx(className);
        String s = name + descriptor;
        if (clzCtx != null) {
            if (clzCtx.getAccessMethod(s) != null) return null;
            if (clzCtx.needExtend(s)) access = Utils.removePrivateAccessFlag(access);
            return new RemoveAccessMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions), transaction);
        } else {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }

}
