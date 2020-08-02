package com.qh.asm;

import java.util.ArrayList;
import java.util.List;

class Node {
    final Class clz;
    final List<Node> childList;

    Node(Class clz) {
        this.clz = clz;
        this.childList = new ArrayList<>();
    }

    void addChild(Node node) {
        this.childList.add(node);
    }
}
