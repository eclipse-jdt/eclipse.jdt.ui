package org.eclipse.jdt.internal.ui.text.java.hover;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.ProblemPosition;
import org.eclipse.jdt.internal.ui.text.HTMLPrinter;



public class JavaProblemHover implements IJavaEditorTextHover {
	
	private ProblemPosition fProblemPosition;
	private CompilationUnitEditor fCompilationUnitEditor;
	
	/**
	 * Creates a new problem hover.
	 */
	public JavaProblemHover() {
	}
	
	/*
	 * Formats a message as HTML text.
	 */
	private String formatMessage(String message) {
		StringBuffer buffer= new StringBuffer();
		HTMLPrinter.addPageProlog(buffer);
		HTMLPrinter.addParagraph(buffer, HTMLPrinter.convertToHTMLContent(message));
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}
	
	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		List problemPositions= fCompilationUnitEditor.getProblemPositions();
		for (Iterator e = problemPositions.iterator(); e.hasNext();) {
			ProblemPosition pp = (ProblemPosition) e.next();
			if (pp.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
				String msg= pp.getProblem().getMessage();
				if (msg != null && msg.trim().length() > 0)
					return formatMessage(msg);
			}
		}
		return null;
	}
	
	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return null;
	}
	
	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		if (editor instanceof CompilationUnitEditor)
			fCompilationUnitEditor= (CompilationUnitEditor) editor;
		else
			fCompilationUnitEditor= null;
	}
}