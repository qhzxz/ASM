package com.qh.asm;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Transaction {
    private Map<String, ClassContext> ctxMap;


    private Transaction(Map<String, ClassContext> ctxMap) {
        this.ctxMap = ctxMap;
    }


    public void execute() throws IOException {
        AccessMethodPicker picker = new AccessMethodPicker(ctxMap);
        picker.pick();
    }

    public static class Builder {
        private Map<String, ClassContext> ctxMap;

        public Builder() {
            this.ctxMap = new HashMap<>();
        }

        public Builder addClass(String clzFilePath) throws IOException {
            AccessMethodDetector detector = new AccessMethodDetector();
            ClassContext detect = detector.detectClass(clzFilePath);
            System.out.println("detect:" + detect);
            ctxMap.put(detect.node.clz.name, detect);
            return this;
        }

        public Builder addJar(String jarFilePath) throws IOException {
            AccessMethodDetector detector = new AccessMethodDetector();
            Map<String, ClassContext> res = detector.detectJar(jarFilePath);
            ctxMap.putAll(res);
            return this;
        }


        public Transaction build() {
            buildExtend();
            filterInvokeMethod();
            return new Transaction(ctxMap);
        }


        private void buildExtend() {
            for (String clz : ctxMap.keySet()) {
                ClassContext clzCtx = ctxMap.get(clz);
                String superName = clzCtx.node.clz.superName;
                ClassContext superCtx = ctxMap.get(superName);
                if (superCtx != null) {
                    superCtx.node.addChild(clzCtx.node);
                }
            }
        }

        private void filterInvokeMethod() {
            for (String clz : ctxMap.keySet()) {
                ClassContext classContext = ctxMap.get(clz);
                Class root = classContext.node.clz;
                List<Node> childList = classContext.node.childList;
                HashSet<String> needToRemove = new HashSet<>();
                Map<String, AccessMethod> accessMethodMap = classContext.accessMethodMap;
                for (String key : accessMethodMap.keySet()) {
                    AccessMethod accessMethod = accessMethodMap.get(key);
                    if (accessMethod.type != AccessType.INVOKE) continue;
                    for (Node childNode : childList) {
                        if (needRemove(root, accessMethod.targetSignature, childNode.clz)) {
                            needToRemove.add(accessMethod.targetSignature);
                            break;
                        }
                    }
                }
            }
        }

        private boolean needRemove(Class root, String signature, Class current) {
            ClassContext classContext = ctxMap.get(current);
            List<Node> mChild = classContext.node.childList;
            for (Node child : mChild) {
                if (needRemove(root, signature, child.clz)) return true;
            }
            return classContext.packageVisibleMethodSet.contains(signature) && Utils.isSamePackage(root, current);
        }

    }
}
