/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextUtilities;

/**
 * Double click strategy aware of Java string and character syntax rules.
 */
public class JavaStringDoubleClickSelector extends JavaDoubleClickSelector {
	
	private String fPartitioning;

	/**
	 * Creates a new Java string double click selector for the given document partitioning.
	 * 
	 * @param partitioning the document partitioning
	 */
	public JavaStringDoubleClickSelector(String partitioning) {
		super();
		fPartitioning= partitioning;
	}
	
	/*
	 * @see ITextDoubleClickStrategy#doubleClicked(ITextViewer)
	 */
	public void doubleClicked(ITextViewer textViewer) {
		
		int offset= textViewer.getSelectedRange().x;
		
		if (offset < 0)
			return;
			
		IDocument document= textViewer.getDocument();
		
		IRegion region= match(document, offset);
		if (region != null && region.getLength() >= 2)
			textViewer.setSelectedRange(region.getOffset() + 1, region.getLength() - 2);
		else
			selectWord(textViewer, document, offset);
	}

	private IRegion match(IDocument document, int offset) {
		try {
			if ((document.getChar(offset) == '"') || (document.getChar(offset) == '\'') ||
				(document.getChar(offset - 1) == '"') || (document.getChar(offset - 1) == '\''))		
			{
				return TextUtilities.getPartition(document, fPartitioning, offset, true);
			}
		} catch (BadLocationException e) {
		}

		return null;
	}
}
