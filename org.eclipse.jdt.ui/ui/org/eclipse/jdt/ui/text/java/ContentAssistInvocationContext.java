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
package org.eclipse.jdt.ui.text.java;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension9;

/**
 * Describes the context of an invocation of content assist in a text viewer. The context knows the
 * document, the invocation offset and can lazily compute the identifier prefix preceding the
 * invocation offset. It may know the viewer.
 * <p>
 * Subclasses may add information to their environment. For example, source code editors may provide
 * specific context information such as an AST.
 * </p>
 * <p>
 * Clients may instantiate and subclass.
 * </p>
 *
 * @since 3.2
 */
public class ContentAssistInvocationContext {

	/* state */
	private final ITextViewer fViewer;
	private final IDocument fDocument;
	private final int fOffset;
	private final boolean fAutoActivated;

	/* cached additional info */
	private CharSequence fPrefix;

	/**
	 * Equivalent to
	 * {@linkplain #ContentAssistInvocationContext(ITextViewer, int, boolean) ContentAssistInvocationContext(viewer, viewer.getSelectedRange().x)}.
	 *
	 * @param viewer the text viewer that content assist is invoked in
	 */
	public ContentAssistInvocationContext(ITextViewer viewer) {
		this(viewer, viewer.getSelectedRange().x);
	}

	/**
	 * Creates a new context for the given viewer and offset.
	 *
	 * @param viewer the text viewer that content assist is invoked in
	 * @param offset the offset into the viewer's document where content assist is invoked at
	 */
	public ContentAssistInvocationContext(ITextViewer viewer, int offset) {
		this(viewer, viewer.getSelectedRange().x, false);
	}

	/**
	 * Creates a new context for the given viewer and offset.
	 *
	 * @param viewer the text viewer that content assist is invoked in
	 * @param offset the offset into the viewer's document where content assist is invoked at
	 * @param autoActivated the current completion session is auto activated or not.
	 *
	 * @since 3.28
	 */
	public ContentAssistInvocationContext(ITextViewer viewer, int offset, boolean autoActivated) {
		Assert.isNotNull(viewer);
		fViewer= viewer;
		fDocument= null;
		fOffset= offset;
		fAutoActivated = autoActivated;
	}

	/**
	 * Creates a new context with no viewer or invocation offset set.
	 */
	protected ContentAssistInvocationContext() {
		fDocument= null;
		fViewer= null;
		fOffset= -1;
		fAutoActivated = false;
	}

	/**
	 * Creates a new context for the given document and offset.
	 *
	 * @param document the document that content assist is invoked in
	 * @param offset the offset into the document where content assist is invoked at
	 */
	public ContentAssistInvocationContext(IDocument document, int offset) {
		this(document, offset, false);
	}

	/**
	 * Creates a new context for the given document and offset.
	 *
	 * @param document the document that content assist is invoked in
	 * @param offset the offset into the document where content assist is invoked at
	 * @param autoActivated the current completion session is auto activated or not.
	 *
	 * @since 3.28
	 */
	public ContentAssistInvocationContext(IDocument document, int offset, boolean autoActivated) {
		Assert.isNotNull(document);
		Assert.isTrue(offset >= 0);
		fViewer= null;
		fDocument= document;
		fOffset= offset;
		fAutoActivated = autoActivated;
	}

	/**
	 * Returns the invocation offset.
	 *
	 * @return the invocation offset
	 */
	public final int getInvocationOffset() {
		return fOffset;
	}

	/**
	 * Returns the viewer, <code>null</code> if not available.
	 *
	 * @return the viewer, possibly <code>null</code>
	 */
	public final ITextViewer getViewer() {
		return fViewer;
	}

	/**
	 * Returns the document that content assist is invoked on, or <code>null</code> if not known.
	 *
	 * @return the document or <code>null</code>
	 */
	public IDocument getDocument() {
		if (fDocument == null) {
			if (fViewer == null) {
				return null;
			}
			return fViewer.getDocument();
		}
		return fDocument;
	}

	/**
	 * Computes the identifier (as specified by {@link Character#isJavaIdentifierPart(char)}) that
	 * immediately precedes the invocation offset.
	 *
	 * @return the prefix preceding the content assist invocation offset, <code>null</code> if
	 *         there is no document
	 * @throws BadLocationException if accessing the document fails
	 */
	public CharSequence computeIdentifierPrefix() throws BadLocationException {
		if (fPrefix == null) {
			IDocument document= getDocument();
			if (document == null) {
				return null;
			}
			int end= getInvocationOffset();
			int start= end;
			while (--start >= 0) {
				if (!Character.isJavaIdentifierPart(document.getChar(start))) {
					break;
				}
			}
			start++;
			fPrefix= document.get(start, end - start);
		}

		return fPrefix;
	}

	/**
	 * Invocation contexts are equal if they describe the same context and are of the same type.
	 * This implementation checks for <code>null</code> values and class equality. Subclasses
	 * should extend this method by adding checks for their context relevant fields (but not
	 * necessarily cached values).
	 * <p>
	 * Example:
	 * </p>
	 *
	 * <pre>
	 * class MyContext extends ContentAssistInvocationContext {
	 * 	private final Object fState;
	 * 	private Object fCachedInfo;
	 *
	 * 	...
	 *
	 * 	public boolean equals(Object obj) {
	 * 		if (!super.equals(obj))
	 * 			return false;
	 * 		MyContext other= (MyContext) obj;
	 * 		return fState.equals(other.fState);
	 * 	}
	 * }
	 * </pre>
	 *
	 * <p>
	 * Subclasses should also extend {@link Object#hashCode()}.
	 * </p>
	 *
	 * @param obj {@inheritDoc}
	 * @return {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (!getClass().equals(obj.getClass())) {
			return false;
		}
		ContentAssistInvocationContext other= (ContentAssistInvocationContext) obj;
		return (fViewer == null && other.fViewer == null || fViewer != null && fViewer.equals(other.fViewer)) && fOffset == other.fOffset && (fDocument == null && other.fDocument == null || fDocument != null && fDocument.equals(other.fDocument));
	}

	/*
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return 23459213 << 5 | (fViewer == null ? 0 : fViewer.hashCode() << 3) | fOffset;
	}

	private boolean inUIThread() {
		return Display.getCurrent() != null;
	}

	/**
	 * Return the current known selection for the viewer, usable in non-UI Thread.
	 * @return the text selection if running in UI-Thread or viewer implements {@link ITextViewerExtension9},
	 *         or <code>null</code> in other cases.
	 * @since 3.20
	 */
	public ITextSelection getTextSelection() {
		ITextViewer viewer = getViewer();
		if (inUIThread()) {
			return (ITextSelection) getViewer().getSelectionProvider().getSelection();
		}
		if (viewer instanceof ITextViewerExtension9) {
			return ((ITextViewerExtension9) viewer).getLastKnownSelection();
		}
		return null;
	}

	/**
	 * Return how the current completion session was triggered.
	 *
	 * @return <code>true</code> if it is triggered automatically.
	 * @since 3.28
	 */
	public boolean isAutoActivated() {
		return fAutoActivated;
	}
}
