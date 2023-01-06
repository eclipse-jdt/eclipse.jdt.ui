/*******************************************************************************
 * Copyright (c) 2018 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.BitSet;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.bcoview.asm.DecompiledClass;
import org.eclipse.jdt.bcoview.asm.DecompilerHelper;
import org.eclipse.jdt.bcoview.asm.DecompilerOptions;
import org.eclipse.jdt.bcoview.ui.JdtUtils;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class TypedElement extends BufferedContent
    implements
        ITypedElement,
        IStructureComparator {

    private final String name;

    private String type;

    private final String methodName;

    private final IJavaElement element;

    /** used by Eclipse to recognize appropriated viewer */
    public static final String TYPE_BYTECODE = "bytecode";

    /** used by Eclipse to recognize appropriated viewer */
    public static final String TYPE_ASM_IFIER = "java";

    private final BitSet modes;

    /**
     * Constructor for TypedElement.
     * @param name
     * @param type
     * @param element
     * @param modes
     */
    public TypedElement(String name, String methodName, String type, IJavaElement element, BitSet modes) {
        super();
        this.name = name;
        this.methodName = methodName;
        this.type = type;
        this.element = element;
        this.modes = modes;
    }

    /**
     * @see org.eclipse.compare.ITypedElement#getName()
     */
    @Override
    public String getName() {
        return name;
    }


    /**
     * @see org.eclipse.compare.ITypedElement#getType()
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * @param type The type to set.
     */
    protected void setType(String type) {
        this.type = type;
    }

    /**
     * @return name
     */
    public String getElementName() {
        return JdtUtils.getElementName(element);
    }

    @Override
    public Image getImage() {
        // default image for .class files
        return CompareUI.getImage("class");
    }

    @Override
    public Object[] getChildren() {
        return new TypedElement[0];
    }

    @Override
    protected InputStream createStream() throws CoreException {
        byte[] classBytes = JdtUtils.readClassBytes(element);
        if (classBytes == null) {
            throw new CoreException(new Status(
                IStatus.ERROR, "org.eclipse.jdt.bcoview", -1,
                "Can't read bytecode for: " + element, null));
        }
        DecompiledClass decompiledClass = null;
        try {
            decompiledClass = DecompilerHelper.getDecompiledClass(
                classBytes, new DecompilerOptions(null, methodName, modes, null));
        } catch (UnsupportedClassVersionError e){
            throw new CoreException(new Status(
                IStatus.ERROR, "org.eclipse.jdt.bcoview", -1,
                "Error caused by attempt to load class compiled with Java version which"
                + " is not supported by current JVM", e));
        }
        final byte[] bytes = decompiledClass.getText().getBytes(Charset.forName("UTF-8"));
        // use internal buffering to prevent multiple calls to this method
        Display.getDefault().syncExec(new Runnable(){
            @Override
            public void run() {
                setContent(bytes);
            }
        });

        return new ByteArrayInputStream(bytes);
    }

    /**
     *
     * @param mode one of BCOConstants.F_* modes
     * @param value
     */
    public void setMode(int mode, boolean value){
        modes.set(mode, value);
        // force create new stream
        discardBuffer();
    }
}
