/*******************************************************************************
 * Copyright (c) 2018 Eric Bruneton and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eric Bruneton - initial API and implementation
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.asm;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.bcoview.preferences.BCOConstants;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;

public class DecompilerHelper  {

    public static DecompiledClass getDecompiledClass(final byte[] bytes,
        DecompilerOptions options) throws UnsupportedClassVersionError {
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode(DecompilerOptions.LATEST_ASM_VERSION);
        int crFlags = 0;
        if(options.modes.get(BCOConstants.F_EXPAND_STACKMAP)) {
            crFlags |= ClassReader.EXPAND_FRAMES;
        }
        cr.accept(cn, crFlags);
        ICommentedClassVisitor printer;
        if (options.modes.get(BCOConstants.F_SHOW_ASMIFIER_CODE)) {
            printer = new CommentedASMifierClassVisitor(cn, options);
        } else {
            printer = new CommentedClassVisitor(cn, options);
        }
        TraceClassVisitor dcv = new TraceClassVisitor(null, (Printer) printer, null);
        cn.accept(dcv);
        return getResult(printer, options, cn);
    }

    private static DecompiledClass getResult(ICommentedClassVisitor printer,  DecompilerOptions options, ClassNode classNode) {
        List<Object> classText = new ArrayList<>();
        formatText(printer.getText(), new StringBuffer(), classText, options.cl);
        while (classText.size() > 0 && classText.get(0).equals("\n")) {
            classText.remove(0);
        }

        DecompiledClassInfo classInfo = printer.getClassInfo();
        return new DecompiledClass(classText, classInfo, classNode);
    }

    private static void formatText(final List<?> input, final StringBuffer line, final List<Object> result,
        final ClassLoader cl) {
        for (int i = 0; i < input.size(); ++i) {
            Object o = input.get(i);
            if (o instanceof List) {
                formatText((List<?>) o, line, result, cl);
            } else if (o instanceof DecompiledMethod) {
                result.add(o);
            } else {
                String s = o.toString();
                int p;
                do {
                    p = s.indexOf('\n');
                    if (p == -1) {
                        line.append(s);
                    } else {
                        result.add(line.toString() + s.substring(0, p + 1));
                        s = s.substring(p + 1);
                        line.setLength(0);
                    }
                } while (p != -1);
            }
        }
    }


}
