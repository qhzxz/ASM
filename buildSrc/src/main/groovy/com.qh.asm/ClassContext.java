package com.qh.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class ClassContext {
    final byte[] data;
    final HashSet<String> packageVisibleMethodSet;
    final String dstPath;
    final Node node;
    final boolean isInJar;

    final Map<String, AccessMethod> accessMethodMap;
    final Set<String> targetSignatureSet;


    ClassContext(byte[] data, String dstPath, Node node, boolean isInJar) {
        this.isInJar = isInJar;
        this.data = data;
        this.dstPath = dstPath;
        this.node = node;

        packageVisibleMethodSet = new HashSet<>();
        accessMethodMap = new HashMap<>();
        targetSignatureSet = new HashSet<>();
    }
}
