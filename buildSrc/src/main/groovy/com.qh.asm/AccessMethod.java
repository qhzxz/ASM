package com.qh.asm;

class AccessMethod {
    final Method accessMethod;
    final String targetSignature;
    final Object target;
    final AccessType type;

    AccessMethod(Method accessMethod, String targetSignature, Object target, AccessType type) {
        this.accessMethod = accessMethod;
        this.targetSignature = targetSignature;
        this.target = target;
        this.type = type;
    }
}
