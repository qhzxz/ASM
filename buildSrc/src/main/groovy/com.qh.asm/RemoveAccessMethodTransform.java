package com.qh.asm;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.tools.r8.graph.F;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
        Transaction.Builder builder = new Transaction.Builder();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
        if (outputProvider != null) outputProvider.deleteAll();
        List<File[]> dirList = new ArrayList<>();
        List<File[]> jarList = new ArrayList<>();
        for (TransformInput in : inputs) {
            Collection<DirectoryInput> directoryInputs = in.getDirectoryInputs();
            System.out.println("====DirectoryInput====");
            for (DirectoryInput d : directoryInputs) {
                System.out.println(String.format("path:%s,name:%s", d.getFile().getAbsolutePath(), d.getName()));
                handleDirInput(d, builder);
                File dest = outputProvider.getContentLocation(d.getName(),
                        d.getContentTypes(), d.getScopes(),
                        Format.DIRECTORY);
                dirList.add(new File[]{d.getFile(), dest});
            }
            System.out.println("====DirectoryInput End====");
            System.out.println("====JarInput====");
            for (JarInput jarInput : in.getJarInputs()) {
                if (jarInput.getFile().getAbsolutePath().endsWith(".jar")) {
                    String jarName = jarInput.getName();
                    String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
                    if (jarName.endsWith(".jar")) {
                        jarName = jarName.substring(0, jarName.length() - 4);
                    }
                    handleJarInput(jarInput, builder);
                    File dest = outputProvider.getContentLocation(jarName + md5Name,
                            jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                    jarList.add(new File[]{jarInput.getFile(), dest});
                }
            }
            System.out.println("====JarInput====");
        }

        Transaction build = builder.build();
        build.execute();

        for (File f[] : dirList) {
            FileUtils.copyDirectoryToDirectory(f[0], f[1]);
        }
        for (File f[] : jarList) {
            FileUtils.copyFile(f[0], f[1]);
        }
    }

    private static void handleDirInput(DirectoryInput input, Transaction.Builder transaction) throws IOException {
        File dir = input.getFile();
        recurseFile(dir, transaction);
    }

    private static void handleJarInput(JarInput input, Transaction.Builder transaction) throws IOException {
        File file = input.getFile();
        transaction.addJar(file.getAbsolutePath());
    }

    private static void recurseFile(File parent, Transaction.Builder transaction) throws IOException {
        File[] files = parent.listFiles();
        for (File child : files) {
            if (child.isDirectory()) {
                recurseFile(child, transaction);
            } else {
                String name = child.getName();
                if (name.endsWith(".class")) {
//                    readClassFile(child, transaction);
                    transaction.addClass(child.getAbsolutePath());
                }
            }
        }
    }


}
