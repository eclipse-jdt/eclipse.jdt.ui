/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.javadoc;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;


/**
 *
 * @since 3.2
 */
public final class JavadocContentAssistInvocationContext extends JavaContentAssistInvocationContext {

	private final int fFlags;

	/**
	 * @param viewer
	 * @param offset
	 * @param editor
	 * @param flags see {@link org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor#RESTRICT_TO_MATCHING_CASE}
	 */
	public JavadocContentAssistInvocationContext(ITextViewer viewer, int offset, IEditorPart editor, int flags) {
		super(viewer, offset, editor);
		fFlags= flags;
	}

	/**
	 * Returns the flags for this content assist invocation.
	 *
	 * @return the flags for this content assist invocation
	 * @see org.eclipse.jdt.ui.text.java.IJavadocCompletionProcessor#RESTRICT_TO_MATCHING_CASE
	 */
	public int getFlags() {
		return fFlags;
	}

	/**
	 * Returns the selection length of the viewer.
	 *
	 * @return the selection length of the viewer
	 */
	public int getSelectionLength() {
		ITextSelection selection = getTextSelection();
		if (selection != null) {
			return selection.getLength();
		}
		return 0;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}

		return fFlags == ((JavadocContentAssistInvocationContext) obj).fFlags;
	}

	/*
	 * @see org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() << 2 | fFlags;
	}

}
