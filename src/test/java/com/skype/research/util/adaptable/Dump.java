/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *
 * Licensed under the MIT license.
 */

package com.skype.research.util.adaptable;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Pretty-print an {@link AdaptableSkipList} container
 */
public class Dump {
    final PrintWriter printWriter;

    public Dump(PrintStream printStream) {
        this(new PrintWriter(printStream));
    }

    public Dump(PrintWriter printWriter) {
        this.printWriter = printWriter;
    }

    public <T> void dump(AdaptableSkipList<T> adaptable) {
        for (int i = 0; i < adaptable.levelCount; ++i) {
            if (i < adaptable.orbitLevel && adaptable.absMinNode.nodes[i] == null) {
                continue; // collapse "atmosphere" levels
            }
            printWriter.printf("Level %d: ", i);
            dump(adaptable, i);
            printWriter.println();
        }
        printWriter.flush();
    }

    private <T> void dump(AdaptableSkipList<T> adaptable, int level) {
        AdaptableSkipList<T>.Node node = adaptable.absMinNode;
        while (node != null) {
            if (node.element != null) {
                printWriter.print(node.element);
            }
            printWriter.print('+');
            dump(node.distances[level]);
            printWriter.print("-> ");
            node = node.nodes[level];
        }
    }

    private void dump(int[] distance) {
        for (int component : distance) {
            printWriter.print(component);
            printWriter.print('/');
        }
    }

    public static <T> void validateIntegrity(FlexibleAdaptable<T> asl) {
        ((AdaptableSkipList<T>) asl).validateIntegrity();
    }
}
