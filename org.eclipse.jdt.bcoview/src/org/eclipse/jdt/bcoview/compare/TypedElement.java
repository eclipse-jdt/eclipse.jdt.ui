/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
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
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import org.eclipse.jdt.bcoview.asm.DecompiledClass;
import org.eclipse.jdt.bcoview.asm.DecompilerHelper;
import org.eclipse.jdt.bcoview.asm.DecompilerOptions;
import org.eclipse.jdt.bcoview.ui.JdtUtils;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.compare.BufferedContent;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.IStructureComparator;

import org.eclipse.jdt.core.IJavaElement;

public class TypedElement extends BufferedContent implements ITypedElement, IStructureComparator {

	private final String name;

	private String type;

	private final String methodName;

	private final IJavaElement element;

	/** used by Eclipse to recognize appropriated viewer */
	public static final String TYPE_BYTECODE = "bytecode"; //$NON-NLS-1$

	/** used by Eclipse to recognize appropriated viewer */
	public static final String TYPE_ASM_IFIER = "java"; //$NON-NLS-1$

	private final BitSet modes;

	public TypedElement(String name, String methodName, String type, IJavaElement element, BitSet modes) {
		super();
		this.name = name;
		this.methodName = methodName;
		this.type = type;
		this.element = element;
		this.modes = modes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

	public String getElementName() {
		return JdtUtils.getElementName(element);
	}

	@Override
	public Image getImage() {
		// default image for .class files
		return CompareUI.getImage("class"); //$NON-NLS-1$
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
					IStatus.ERROR, "org.eclipse.jdt.bcoview", -1, //$NON-NLS-1$
					"Can't read bytecode for: " + element, null)); //$NON-NLS-1$
		}
		DecompiledClass decompiledClass = null;
		try {
			decompiledClass = DecompilerHelper.getDecompiledClass(classBytes, new DecompilerOptions(null, methodName, modes));
		} catch (UnsupportedClassVersionError e) {
			throw new CoreException(new Status(
					IStatus.ERROR, "org.eclipse.jdt.bcoview", -1, //$NON-NLS-1$
					"Error caused by attempt to load class compiled with Java version which" //$NON-NLS-1$
					+ " is not supported by current JVM", //$NON-NLS-1$
					e));
		}
		final byte[] bytes = decompiledClass.getText().getBytes(StandardCharsets.UTF_8);
		// use internal buffering to prevent multiple calls to this method
		Display.getDefault().syncExec(() -> setContent(bytes));

		return new ByteArrayInputStream(bytes);
	}

	/**
	 * @param mode one of BCOConstants.F_* modes
	 * @param value to set
	 */
	public void setMode(int mode, boolean value) {
		modes.set(mode, value);
		// force create new stream
		discardBuffer();
	}
}
