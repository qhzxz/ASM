package com.qh.asm;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;


import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class RemoveAccessMethodTransform extends Transform {
    @Override
    public String getName() {
        return "RemoveAccessMethodTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        Set<QualifiedContent.ContentType> set = new HashSet<>();
        set.add(QualifiedContent.DefaultContentType.CLASSES);
        return set;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);
        System.out.println("开始转换");
        Transaction transaction = new Transaction();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        for (TransformInput in : inputs) {
            Collection<DirectoryInput> directoryInputs = in.getDirectoryInputs();
            System.out.println("====DirectoryInput====");
            for (DirectoryInput d : directoryInputs) {
                System.out.println(String.format("path:%s,name:%s", d.getFile().getAbsolutePath(), d.getName()));
                handleDirInput(d, outputProvider, transaction);
            }
            System.out.println("====DirectoryInput End====");
        }
        transaction.remove();
    }

    private static void handleDirInput(DirectoryInput input, TransformOutputProvider provider, Transaction transaction) {
        File dir = input.getFile();
        recurseFile(dir, transaction);
    }

    private static void handleJarInput(JarInput input, TransformOutputProvider provider, Transaction transaction) {
        File file = input.getFile();
        recurseFile(file, transaction);
    }

    private static void recurseFile(File parent, Transaction transaction) {
        File[] files = parent.listFiles();
        for (File child : files) {
            if (child.isDirectory()) {
                recurseFile(child, transaction);
            } else {
                String name = child.getName();
                if (name.endsWith(".class")) {
                    readClassFile(child, transaction);
                }
            }
        }
    }

    private static void readClassFile(File file, Transaction transaction) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte data[] = new byte[(int) file.length()];
            inputStream.read(data);
            ClassReader reader = new ClassReader(data);
            Class clz = new Class(reader.getAccess(), reader.getClassName(), reader.getSuperName(), reader.getInterfaces());
            System.out.println("class:" + clz.toString());
            Transaction.Node node = new Transaction.Node(clz);
            Transaction.ClassContext clzContext = new Transaction.ClassContext(data, file.getAbsolutePath(), node);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            ClassVisitor visitor = new AccessClassVisitor(writer, clzContext);
            reader.accept(visitor, ClassReader.EXPAND_FRAMES);
            transaction.putClass(clz, clzContext);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
