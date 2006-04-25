/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.text.JavaWordFinder;


/**
 * Java element hyperlink detector.
 *
 * @since 3.1
 */
public class JavaElementHyperlinkDetector implements IHyperlinkDetector {

	private ITextEditor fTextEditor;

	/**
	 * Creates a new Java element hyperlink detector.
	 *
	 * @param editor the editor in which to detect the hyperlink
	 */
	public JavaElementHyperlinkDetector(ITextEditor editor) {
		Assert.isNotNull(editor);
		fTextEditor= editor;
	}

	/*
	 * @see org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion, boolean)
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region, boolean canShowMultipleHyperlinks) {
		if (region == null || canShowMultipleHyperlinks || !(fTextEditor instanceof JavaEditor))
			return null;

		IAction openAction= fTextEditor.getAction("OpenEditor"); //$NON-NLS-1$
		if (openAction == null)
			return null;

		int offset= region.getOffset();

		IJavaElement input= EditorUtility.getEditorInputJavaElement(fTextEditor, false);
		if (input == null)
			return null;

		try {
			IDocument document= fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());
			IRegion wordRegion= JavaWordFinder.findWord(document, offset);
			if (wordRegion == null)
				return null;
			
			IJavaElement[] elements= null;
			elements= ((ICodeAssist) input).codeSelect(wordRegion.getOffset(), wordRegion.getLength());
			if (elements != null && elements.length > 0)
				return new IHyperlink[] {new JavaElementHyperlink(wordRegion, openAction)};
		} catch (JavaModelException e) {
			return null;
		}

		return null;
	}
}
