package com.qh.asm;

import org.objectweb.asm.Opcodes;

public class Utils {


    static boolean isContainSyntheticFlag(int flag) {
        return ((flag >> 12) & 1) == 1;
    }

    static boolean isContainStaticFlag(int flag) {
        return ((flag >> 3) & 1) == 1;
    }

    static boolean isEmpty(String string) {
        return string == null || string.length() == 0;
    }

    static int removePrivateAccessFlag(int acc) {
        acc = acc & 0xffff;
        int res = 0;
        int i = 0;
        while (i < 16) {
            if (((acc >> i) & 1) != 0 && i != 1) {
                res |= (1 << i);
            }
            i++;
        }
        return res;
    }

    //0000
    public static boolean isContainPrivateAccessFlag(int flag) {
        flag = flag & 0xffff;
        return ((flag >> 1) & 1) != 0;
    }

    static boolean isContainStaticAccessFlag(int flag) {
        flag = flag & 0xffff;
        return ((flag >> 3) & 1) != 0;
    }

    static boolean isSamePackage(Class a, Class b) {
        if (a == null || b == null) return false;
        int aIndex = a.name.lastIndexOf("/");
        if (aIndex == -1) return false;
        int bIndex = b.name.lastIndexOf("/");
        if (bIndex == -1) return false;
        return a.name.substring(0, aIndex).equals(b.name.substring(0, bIndex));
    }

    static boolean isDoubleWordType(String type) {
        if (isEmpty(type)) return false;
        return type.equals("D") || type.equals("J");
    }

    static boolean isAddOpcode(int opCode) {
        return opCode == Opcodes.IADD || opCode == Opcodes.LADD || opCode == Opcodes.FADD || opCode == Opcodes.DADD;
    }

    static boolean isSubOpcode(int opCode) {
        return opCode == Opcodes.ISUB || opCode == Opcodes.LSUB || opCode == Opcodes.FSUB || opCode == Opcodes.DSUB;
    }
}
