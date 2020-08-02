package com.qh.asm;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class AccessMethodDetector {


    ClassContext detect(String filePath) {
        File file = new File(filePath);
        if (file.exists()) return null;
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] bytes = IOUtils.toByteArray(inputStream);
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            AccessClassVisitor visitor = new AccessClassVisitor(writer);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            Class clz = new Class(reader.getAccess(), reader.getClassName(), reader.getSuperName(), reader.getInterfaces());
            Node node = new Node(clz);
            ClassContext classContext = new ClassContext(bytes, filePath, node);
            return classContext;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private class AccessClassVisitor extends ClassVisitor implements Opcodes {
        private HashMap<String, Object> mPrivateTarget = new HashMap<>();
        private List<AccessMethodVisitor> mMethodVisitorList = new ArrayList<>();
        private List<String> mPackageVisibleMethodSet = new ArrayList<>();

        public AccessClassVisitor(ClassVisitor visitor) {
            super(Opcodes.ASM6, visitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (Utils.isContainPrivateAccessFlag(access)) {
                Field field = new Field(access, name, descriptor, signature, value);
                mPrivateTarget.put(field.name, field);
            }

            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            Method method = new Method(access, name, descriptor, signature, exceptions);
            boolean containPrivate = Utils.isContainPrivateAccessFlag(access);
            boolean containStatic = Utils.isContainStaticAccessFlag(access);
            boolean containSynthetic = Utils.isContainSyntheticFlag(access);
            if (containPrivate) {
                mPrivateTarget.put(method.getSignature(), method);
            }

            if (!containPrivate && !containStatic) {
                mPackageVisibleMethodSet.add(method.getSignature());
            }
            MethodVisitor visitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (containStatic && containSynthetic && name.startsWith("access$") && (exceptions == null || exceptions.length == 0)) {
                AccessMethodVisitor accessMethodVisitor = new AccessMethodVisitor(visitor, method);
                mMethodVisitorList.add(accessMethodVisitor);
                return accessMethodVisitor;
            } else {
                return visitor;
            }
        }
    }

    private class AccessMethodVisitor extends MethodVisitor {
        private Method mMethod;
        private int mVarCount;
        private String mTargetName;
        private String mTargetDescriptor;
        private int readFieldOpCodeCount;
        private int writeFieldOpCodeCount;
        private int invokeMethodOpCodeCount;
        private int addOpcodeCount;
        private int subOpcodeCount;


        public AccessMethodVisitor(MethodVisitor methodVisitor, Method method) {
            super(Opcodes.ASM6, methodVisitor);
            mMethod = method;
        }


        @Override
        public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            super.visitLocalVariable(name, descriptor, signature, start, end, index);
            mVarCount++;
        }


        private boolean isSetAccessMethod() {
            boolean isInvokeOtherMethod = invokeMethodOpCodeCount > 0;
            boolean isReadWriteCountValid = writeFieldOpCodeCount == 1 && readFieldOpCodeCount == 0;
            boolean isAddSubOpcodeCountValid = addOpcodeCount == 0 && subOpcodeCount == 0;
            boolean isVariableCountValid = mVarCount <= 2;
            boolean isAccessFieldValid = !Utils.isEmpty(mTargetName) && !Utils.isEmpty(mTargetDescriptor);
            return isReadWriteCountValid && isVariableCountValid && isAccessFieldValid && isAddSubOpcodeCountValid && !isInvokeOtherMethod;
        }

        private boolean isGetAccessMethod() {
            boolean isInvokeOtherMethod = invokeMethodOpCodeCount > 0;
            boolean isReadWriteCountValid = writeFieldOpCodeCount == 0 && readFieldOpCodeCount == 1;
            boolean isVariableCountValid = mVarCount <= 1;
            boolean isAddSubOpcodeCountValid = addOpcodeCount == 0 && subOpcodeCount == 0;
            boolean isFieldValid = !Utils.isEmpty(mTargetName) && !Utils.isEmpty(mTargetDescriptor);
            boolean isReturnDescriptorValid = isFieldValid && mMethod.descriptor.endsWith(mTargetDescriptor);
            return isReadWriteCountValid && isVariableCountValid && isFieldValid && isReturnDescriptorValid && isAddSubOpcodeCountValid && !isInvokeOtherMethod;

        }

        private boolean isInvokeAccessMethod() {
            boolean isInvokeMethodCountValid = invokeMethodOpCodeCount == 1;
            boolean isReadWriteValid = writeFieldOpCodeCount == 0 && readFieldOpCodeCount == 0;
            boolean isAddSubOpcodeCountValid = addOpcodeCount == 0 && subOpcodeCount == 0;
            boolean isMethodValid = !Utils.isEmpty(mTargetName) && !Utils.isEmpty(mTargetDescriptor);
            boolean isReturnTypeValid = isMethodValid && mTargetDescriptor.substring(mTargetDescriptor.indexOf(")") + 1).equals(mMethod.getReturnType());
            return isInvokeMethodCountValid && isReadWriteValid && isMethodValid && isReturnTypeValid && isAddSubOpcodeCountValid;
        }

        private boolean isAddAccessMethod() {
            boolean isInvokeOtherMethod = invokeMethodOpCodeCount > 0;
            boolean isReadWriteCountValid = writeFieldOpCodeCount == 1 && readFieldOpCodeCount == 1;
            boolean isVariableCountValid = mVarCount <= 1;
            boolean isAddSubOpcodeCountValid = addOpcodeCount == 1 && subOpcodeCount == 0;
            boolean isFieldValid = !Utils.isEmpty(mTargetName) && !Utils.isEmpty(mTargetDescriptor);
            boolean isReturnDescriptorValid = isFieldValid && mMethod.descriptor.endsWith(mTargetDescriptor);
            return isReadWriteCountValid && isVariableCountValid && isFieldValid && isReturnDescriptorValid && isAddSubOpcodeCountValid && !isInvokeOtherMethod;
        }

        private boolean isSubAccessMethod() {
            boolean isInvokeOtherMethod = invokeMethodOpCodeCount > 0;
            boolean isReadWriteCountValid = writeFieldOpCodeCount == 1 && readFieldOpCodeCount == 1;
            boolean isVariableCountValid = mVarCount <= 1;
            boolean isAddSubOpcodeCountValid = addOpcodeCount == 1 && subOpcodeCount == 0;
            boolean isFieldValid = !Utils.isEmpty(mTargetName) && !Utils.isEmpty(mTargetDescriptor);
            boolean isReturnDescriptorValid = isFieldValid && mMethod.descriptor.endsWith(mTargetDescriptor);
            return isReadWriteCountValid && isVariableCountValid && isFieldValid && isReturnDescriptorValid && isAddSubOpcodeCountValid && !isInvokeOtherMethod;

        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            super.visitFieldInsn(opcode, owner, name, descriptor);
            if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
                readFieldOpCodeCount++;
            } else if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTSTATIC) {
                writeFieldOpCodeCount++;
            }
            mTargetName = name;
            mTargetDescriptor = descriptor;
        }

        @Override
        public void visitInsn(int opcode) {
            super.visitInsn(opcode);
            if (Utils.isAddOpcode(opcode)) {
                addOpcodeCount++;
            } else if (Utils.isSubOpcode(opcode)) {
                subOpcodeCount++;
            }
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (!isInterface) {
                mTargetName = name;
                mTargetDescriptor = descriptor;
            }
            invokeMethodOpCodeCount++;
        }


        private Method getAccessMethod() {
            return mMethod;
        }


        private String getTargetSignature() {
            return mTargetName + mTargetDescriptor;
        }

        private AccessType getAccessType() {
            if (isInvokeAccessMethod()) {
                return AccessType.INVOKE;
            } else if (isSetAccessMethod()) {
                return AccessType.SET;
            } else if (isGetAccessMethod()) {
                return AccessType.GET;
            } else if (isAddAccessMethod()) {
                return AccessType.INC;
            } else if (isSubAccessMethod()) {
                return AccessType.DEC;
            }
            return AccessType.NONE;
        }
    }
}
