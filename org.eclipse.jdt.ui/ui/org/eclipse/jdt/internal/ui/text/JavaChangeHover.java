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
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineChangeHover;

/**
 * A line change hover for Java source code. Adds a custom information control creator returning a
 * source viewer with syntax coloring.
 * 
 * @since 3.0
 */
public class JavaChangeHover extends LineChangeHover  {
	
	/** The last computet partition type. */
	String fPartition;
	/** The last created information control. */
	CustomSourceInformationControl fInformationControl;
	/** The document partitioning to be used by this hover. */
	private String fPartitioning;
	
	/**
	 * Creates a new change hover for the given document partitioning.
	 * 
	 * @param partitioning the document partitioning
	 */
	public JavaChangeHover(String partitioning) {
		fPartitioning= partitioning;
	}
	
	/*
	 * @see org.eclipse.ui.internal.editors.text.LineChangeHover#formatSource(java.lang.String)
	 */
	protected String formatSource(String content) {
		return content;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.IAnnotationHoverExtension#getInformationControlCreator()
	 */
	public IInformationControlCreator getInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				fInformationControl= new CustomSourceInformationControl(parent, fPartition);
				return fInformationControl;
			}
		};
	}
	
	/*
	 * @see org.eclipse.jface.text.source.LineChangeHover#computeLineRange(org.eclipse.jface.text.source.ISourceViewer, int, int, int)
	 */
	protected Point computeLineRange(ISourceViewer viewer, int line, int first, int number) {
		Point lineRange= super.computeLineRange(viewer, line, first, number);
		if (lineRange != null) {
			fPartition= getPartition(viewer, lineRange.x);
		} else {
			fPartition= IDocument.DEFAULT_CONTENT_TYPE;
		}
		if (fInformationControl != null)
			fInformationControl.setStartingPartitionType(fPartition); 
		return lineRange;
	}

	/**
	 * Returns the partition type of the document displayed in <code>viewer</code> at <code>startLine</code>.

	 * @param viewer the viewer
	 * @param startLine the line in the viewer
	 * @return the partition type at the start of <code>startLine</code>, or <code>IDocument.DEFAULT_CONTENT_TYPE</code> if none can be detected
	 */
	private String getPartition(ISourceViewer viewer, int startLine) {
		if (viewer == null)
			return null;
		IDocument doc= viewer.getDocument();
		if (doc == null)
			return null;
		if (startLine <= 0)
			return IDocument.DEFAULT_CONTENT_TYPE;
		try {
			ITypedRegion region= TextUtilities.getPartition(doc, fPartitioning, doc.getLineOffset(startLine) - 1, true);
			return region.getType();
		} catch (BadLocationException e) {
		}
		return IDocument.DEFAULT_CONTENT_TYPE;
	}
}
