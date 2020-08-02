package com.qh.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Transaction {
    private Map<String, ClassContext> ctxMap;


    public Transaction() {
        ctxMap = new HashMap<>();
    }


    public void putClass(String clz, ClassContext context) {
        ctxMap.put(clz, context);
    }

    public void remove() {
        buildExtend();
        filterInvokeMethod();

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
                if (accessMethod.type!=AccessType.INVOKE) continue;
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

    public ClassContext getClzCtx(String clzName) {
        return ctxMap.get(clzName);
    }
}
