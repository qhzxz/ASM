package com.qh.asm;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class AccessMethodPicker {
    private final Map<String, ClassContext> mMap;

    AccessMethodPicker(Map<String, ClassContext> mMap) {
        this.mMap = mMap;
    }

    void pick() throws IOException {
        HashMap<String, HashMap<String, byte[]>> jarMap = new HashMap<>();
        for (String key : mMap.keySet()) {
            ClassContext classContext = mMap.get(key);
            ClassReader reader = new ClassReader(classContext.data);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            AccessMethodClassPicker classPicker = new AccessMethodClassPicker(writer);
            reader.accept(classPicker, ClassReader.EXPAND_FRAMES);
            byte[] bytes = writer.toByteArray();
            if (!classContext.isInJar) {
                HashMap<String, byte[]> stringHashMap = jarMap.get(classContext.dstPath);
                if (stringHashMap == null) {
                    stringHashMap = new HashMap<>();
                    jarMap.put(classContext.dstPath, stringHashMap);
                }
                stringHashMap.put(reader.getClassName(), bytes);
            } else {
                writeFile(classContext.dstPath, bytes);
            }
        }

        for (String key : jarMap.keySet()) {
            HashMap<String, byte[]> stringHashMap = jarMap.get(key);
            writeJar(key, stringHashMap);
        }
    }

    private void writeJar(String dstPath, HashMap<String, byte[]> map) throws IOException {
        File dstFile = new File(dstPath);
        JarFile jarFile = new JarFile(dstFile);
        File tmpFile = new File(dstFile.getParentFile().getAbsolutePath() + File.separator + "classes_temp.jar");
        //避免上次的缓存被重复插入
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(tmpFile));
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry jarEntry = entries.nextElement();
            String name = jarEntry.getName();
            ZipEntry zipEntry = new ZipEntry(name);
            jarOutputStream.putNextEntry(zipEntry);
            if (map.containsKey(name)) {
                byte[] bytes = map.get(name);
                IOUtils.write(bytes, jarOutputStream);
            } else {
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                IOUtils.write(IOUtils.toByteArray(inputStream), jarOutputStream);
                IOUtils.closeQuietly(inputStream);
            }
            jarOutputStream.closeEntry();
        }
        IOUtils.closeQuietly(jarOutputStream);
        IOUtils.closeQuietly(jarFile);
        dstFile.delete();
        tmpFile.renameTo(dstFile);
    }

    private void writeFile(String dstPath, byte[] data) throws IOException {
        File file = new File(dstPath);
        FileOutputStream outputStream = new FileOutputStream(file);
        IOUtils.write(data, outputStream);
        IOUtils.closeQuietly(outputStream);
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
            if (classContext != null && classContext.targetSignatureSet.contains(name + descriptor)) {
                access = Utils.removePrivateAccessFlag(access);
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            ClassContext classContext = mMap.get(className);
            if (classContext != null && classContext.targetSignatureSet.contains(name + descriptor)) {
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
                    boolean doubleWordType = isDoubleWordType(field.descriptor);
                    boolean isStatic = Utils.isContainStaticAccessFlag(field.access);
                    if (type == AccessType.GET) {
                        mv.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner, field.name, field.descriptor);
                    } else {
                        if (type == AccessType.SET) {
                            if (!isStatic) {
                                mv.visitInsn(doubleWordType ? Opcodes.DUP2_X1 : Opcodes.DUP_X1);
                            } else {
                                mv.visitInsn(doubleWordType ? Opcodes.DUP2 : Opcodes.DUP);
                            }
                        } else {
                            if (!isStatic) mv.visitInsn(Opcodes.DUP);
                            mv.visitFieldInsn(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner, field.name, field.descriptor);
                            if (!isStatic) {
                                mv.visitInsn(doubleWordType ? Opcodes.DUP2_X1 : Opcodes.DUP_X1);
                            } else {
                                mv.visitInsn(doubleWordType ? Opcodes.DUP2 : Opcodes.DUP);
                            }

                            int oneCode = getOneByType(field.descriptor);
                            if (oneCode == -1) throw new IllegalStateException();
                            mv.visitInsn(oneCode);
                            if (type == AccessType.INC) {
                                int addCodeByType = getAddCodeByType(field.descriptor);
                                if (addCodeByType == -1) throw new IllegalStateException();
                                mv.visitInsn(addCodeByType);
                            } else if (type == AccessType.DEC) {
                                int subCodeByType = getSubCodeByType(field.descriptor);
                                if (subCodeByType == -1) throw new IllegalStateException();
                                mv.visitInsn(subCodeByType);
                            } else {
                                throw new IllegalStateException();
                            }
                            if (needCast(field.descriptor)) {
                                int castCodeByType = getCastCodeByType(field.descriptor);
                                if (castCodeByType == -1) throw new IllegalStateException();
                                mv.visitInsn(castCodeByType);
                            }
                        }
                        mv.visitFieldInsn(isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, owner, field.name, field.descriptor);
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

    private boolean isDoubleWordType(String fieldDescriptor) {
        return fieldDescriptor.equals("D") || fieldDescriptor.equals("J");
    }

    private int getOneByType(String fieldDescriptor) {
        if (fieldDescriptor.equals("I") || fieldDescriptor.equals("B") || fieldDescriptor.equals("S") || fieldDescriptor.equals("C")) {
            return Opcodes.ICONST_1;
        } else if (fieldDescriptor.equals("F")) {
            return Opcodes.FCONST_1;
        } else if (fieldDescriptor.equals("D")) {
            return Opcodes.DCONST_1;
        } else if (fieldDescriptor.equals("J")) {
            return Opcodes.LCONST_1;
        }
        return -1;
    }

    private int getAddCodeByType(String fieldDescriptor) {
        if (fieldDescriptor.equals("I") || fieldDescriptor.equals("B") || fieldDescriptor.equals("S") || fieldDescriptor.equals("C")) {
            return Opcodes.IADD;
        } else if (fieldDescriptor.equals("F")) {
            return Opcodes.FADD;
        } else if (fieldDescriptor.equals("D")) {
            return Opcodes.DADD;
        } else if (fieldDescriptor.equals("J")) {
            return Opcodes.LADD;
        }
        return -1;
    }

    private int getSubCodeByType(String fieldDescriptor) {
        if (fieldDescriptor.equals("I") || fieldDescriptor.equals("B") || fieldDescriptor.equals("S") || fieldDescriptor.equals("C")) {
            return Opcodes.ISUB;
        } else if (fieldDescriptor.equals("F")) {
            return Opcodes.FSUB;
        } else if (fieldDescriptor.equals("D")) {
            return Opcodes.DSUB;
        } else if (fieldDescriptor.equals("J")) {
            return Opcodes.LSUB;
        }
        return -1;
    }

    private boolean needCast(String fieldDescriptor) {
        return fieldDescriptor.equals("B") || fieldDescriptor.equals("S") || fieldDescriptor.equals("C");
    }

    private int getCastCodeByType(String fieldDescriptor) {
        if (fieldDescriptor.equals("B")) {
            return Opcodes.I2B;
        } else if (fieldDescriptor.equals("S")) {
            return Opcodes.I2S;
        } else if (fieldDescriptor.equals("C")) {
            return Opcodes.I2C;
        }
        return -1;
    }
}

