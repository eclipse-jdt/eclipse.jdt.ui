/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITypedRegion;

/**
 * Double click strategy aware of Java string and character syntax rules.
 */
public class JavaStringDoubleClickSelector extends JavaDoubleClickSelector {

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
				return document.getPartition(offset);
			}
		} catch (BadLocationException e) {
		}

		return null;
	}
}
