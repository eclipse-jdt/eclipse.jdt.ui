/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.action.IAction;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICodeAssist;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.actions.SelectionConverter;


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
	 * @see org.eclipse.jdt.internal.ui.javaeditor.IHyperlinkDetector#detectHyperlink(org.eclipse.jface.text.IRegion)
	 * @since 3.1
	 */
	public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region) {
		if (region == null || !(fTextEditor instanceof JavaEditor))
			return null;
		
		IAction openAction= fTextEditor.getAction("OpenEditor"); //$NON-NLS-1$
		if (openAction == null)
			return null;
	
		int offset= region.getOffset();				

		IJavaElement input= SelectionConverter.getInput((JavaEditor)fTextEditor);
		if (input == null)
			return null;

		try {
			
			IJavaElement[] elements= null;
			synchronized (input) {
				elements= ((ICodeAssist) input).codeSelect(offset, 0);
			}
			
			IDocument document= fTextEditor.getDocumentProvider().getDocument(fTextEditor.getEditorInput());
			
			if (elements != null && elements.length > 0)
				return new IHyperlink[] {new JavaElementHyperlink(selectWord(document, offset), openAction)};
		} catch (JavaModelException e) {
			return null;	
		}
		
		return null;
	}
	
	private IRegion selectWord(IDocument document, int anchor) {
	
		try {		
			int offset= anchor;
			char c;

			while (offset >= 0) {
				c= document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				--offset;
			}

			int start= offset;

			offset= anchor;
			int length= document.getLength();

			while (offset < length) {
				c= document.getChar(offset);
				if (!Character.isJavaIdentifierPart(c))
					break;
				++offset;
			}
			
			int end= offset;
			
			if (start == end)
				return new Region(start, 0);
			else
				return new Region(start + 1, end - start - 1);
			
		} catch (BadLocationException x) {
			return null;
		}
	}
}
