/*******************************************************************************
 * Copyright (c) 2018 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Angelo Zerr: initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import java.util.function.Consumer;

import org.eclipse.swt.events.MouseEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Abstract class for Java code mining.
 *
 * @since 3.16
 */
public abstract class AbstractJavaElementLineHeaderCodeMining extends LineHeaderCodeMining {

	private final IJavaElement element;

	public AbstractJavaElementLineHeaderCodeMining(IJavaElement element, IDocument document, ICodeMiningProvider provider,
			Consumer<MouseEvent> action) throws JavaModelException, BadLocationException {
		super(getLineNumber(element, document), document, provider, action);
		this.element= element;
	}

	private static int getLineNumber(IJavaElement element, IDocument document)
			throws JavaModelException, BadLocationException {
		ISourceRange r= ((ISourceReference) element).getNameRange();
		int offset= r.getOffset();
		return document.getLineOfOffset(offset);
	}

	/**
	 * Returns the java element.
	 *
	 * @return the java element.
	 */
	public IJavaElement getElement() {
		return element;
	}

}
