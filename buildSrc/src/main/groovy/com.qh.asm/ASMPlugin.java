package com.qh.asm;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class ASMPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("====ASM插件====");
        System.out.println("display name:" + project.getDisplayName());
        System.out.println("name:" + project.getName());
        System.out.println("====ASM插件====");

        AppExtension extension = project.getExtensions().getByType(AppExtension.class);
        extension.registerTransform(new RemoveAccessMethodTransform());
    }
}
