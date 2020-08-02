package com.qh.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class RemoveAccessMethodVisitor extends MethodVisitor {
    private Transaction transaction;

    public RemoveAccessMethodVisitor(MethodVisitor methodVisitor, Transaction ctx) {
        super(Opcodes.ASM4, methodVisitor);
        transaction = ctx;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        System.out.println("visitMethodInsn");
        Transaction.ClassContext clzCtx = transaction.getClzCtx(owner);
        if (clzCtx != null) {
            Method accessMethod = clzCtx.getAccessMethod(name + descriptor);
            if (opcode == Opcodes.INVOKESTATIC && accessMethod != null && !isInterface) {
                System.out.println("visitMethodInsn:" + accessMethod.toString());
                String s = clzCtx.invokeMap.get(accessMethod);
                System.out.println("visitMethodInsn:" + s);
                if (!Utils.isEmpty(s)) {
                    Method method = clzCtx.methodMap.get(s);
                    if (Utils.isContainStaticAccessFlag(method.access)) {
                        mv.visitMethodInsn(Opcodes.INVOKESTATIC, owner, method.name, method.descriptor, false);
                    } else {
                        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, owner, method.name, method.descriptor, false);
                    }
                    return;
                }
                s = clzCtx.readMap.get(accessMethod);
                if (!Utils.isEmpty(s)) {
                    Field field = clzCtx.fieldMap.get(s);
                    if (Utils.isContainStaticAccessFlag(field.access)) {
                        mv.visitFieldInsn(Opcodes.GETSTATIC, owner, field.name, field.descriptor);
                    } else {
                        mv.visitFieldInsn(Opcodes.GETFIELD, owner, field.name, field.descriptor);
                    }
                    return;
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, false);
            } else {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
