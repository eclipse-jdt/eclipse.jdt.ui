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

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension;

import org.eclipse.jdt.internal.ui.text.java.hover.SourceViewerInformationControl;

/**
 * Source viewer used to display quick diff hovers.
 * 
 * @since 3.0
 */
public class CustomSourceInformationControl extends SourceViewerInformationControl {

	/** The font name for the viewer font - the same as the java editor's. */
	private static final String SYMBOLIC_FONT_NAME= "org.eclipse.jdt.ui.editors.textfont"; //$NON-NLS-1$
	
	/** The maximum width of the control, set in <code>setSizeConstraints(int, int)</code>. */
	int fMaxWidth= Integer.MAX_VALUE;
	/** The maximum height of the control, set in <code>setSizeConstraints(int, int)</code>. */
	int fMaxHeight= Integer.MAX_VALUE;

	/** The partition type to be used as the starting partition type by the paritition scanner. */
	private String fPartition;
	/** The horizontal scroll index. */
	private int fHorizontalScrollPixel;
	
	/*
	 * @see org.eclipse.jface.text.IInformationControl#setSizeConstraints(int, int)
	 */
	public void setSizeConstraints(int maxWidth, int maxHeight) {
		fMaxWidth= maxWidth;
		fMaxHeight= maxHeight;
	}

	/**
	 * Creates a new information control.
	 * 
	 * @param parent the shell that is the parent of this hover / control
	 * @param partition the initial partition type to be used for the underlying viewer
	 */
	public CustomSourceInformationControl(Shell parent, String partition) {
		super(parent);
		setViewerFont();
		setStartingPartitionType(partition);
	}
	
	/*
	 * @see org.eclipse.jface.text.IInformationControl#computeSizeHint()
	 */
	public Point computeSizeHint() {
		Point size= super.computeSizeHint();
		size.x= Math.min(size.x, fMaxWidth);
		size.y= Math.min(size.y, fMaxHeight);
		return size;
	}

	/**
	 * Sets the font for this viewer sustaining selection and scroll position.
	 */
	private void setViewerFont() {
		Font font= JFaceResources.getFont(SYMBOLIC_FONT_NAME);

		if (getViewer().getDocument() != null) {

			Point selection= getViewer().getSelectedRange();
			int topIndex= getViewer().getTopIndex();
			
			StyledText styledText= getViewer().getTextWidget();
			Control parent= styledText;
			if (getViewer() instanceof ITextViewerExtension) {
				ITextViewerExtension extension= (ITextViewerExtension) getViewer();
				parent= extension.getControl();
			}
			
			parent.setRedraw(false);
			
			styledText.setFont(font);
			
			getViewer().setSelectedRange(selection.x , selection.y);
			getViewer().setTopIndex(topIndex);
			
			if (parent instanceof Composite) {
				Composite composite= (Composite) parent;
				composite.layout(true);
			}
			
			parent.setRedraw(true);
			
		} else {
			StyledText styledText= getViewer().getTextWidget();
			styledText.setFont(font);
		}	
	}

	/**
	 * Sets the initial partition for the underlying source viewer.
	 * 
	 * @param partition the partition type
	 */
	public void setStartingPartitionType(String partition) {
		if (partition == null)
			fPartition= IDocument.DEFAULT_CONTENT_TYPE;
		else
			fPartition= partition;
	}
	
	/*
	 * @see org.eclipse.jface.text.IInformationControl#setInformation(java.lang.String)
	 */
	public void setInformation(String content) {
		String spaces= getSpacesForHorizontalScrolling();
		
		super.setInformation(content + spaces);
		IDocument doc= getViewer().getDocument();
		if (doc == null)
			return;
		
		String start= null;
		if (IJavaPartitions.JAVA_DOC.equals(fPartition)) {
			start= "/**" + doc.getLegalLineDelimiters()[0]; //$NON-NLS-1$
		} else if (IJavaPartitions.JAVA_MULTI_LINE_COMMENT.equals(fPartition)) {
			start= "/*" + doc.getLegalLineDelimiters()[0]; //$NON-NLS-1$
		}
		if (start != null) {
			try {
				doc.replace(0, 0, start);
				int startLen= start.length();
				getViewer().setDocument(doc, startLen, doc.getLength() - startLen);
			} catch (BadLocationException e) {
			}
		}
		
		getViewer().getTextWidget().setHorizontalPixel(fHorizontalScrollPixel);
	}

	/**
	 * Returns a run of spaces the length of which is at least
	 * <code>fHorizontalScrollPixel</code>.
	 * 
	 * @return the spaces to add to the document content to ensure that it can
	 *         be scrolled at least <code>fHorizontalScrollPixel</code>
	 */
	private String getSpacesForHorizontalScrolling() {
		StyledText widget= getViewer().getTextWidget();
		GC gc= new GC(widget);
		StringBuffer spaces= new StringBuffer();
		Point spaceSize= gc.stringExtent(" "); //$NON-NLS-1$
		gc.dispose();
		int n= fHorizontalScrollPixel / spaceSize.x + 1; 
		for (int i= 0; i < n; i++)
			spaces.append(' ');
		return spaces.toString();
	}
	
	/**
	 * Sets the horizontal scroll index in pixels.
	 *  
	 * @param scrollIndex the new horizontal scroll index
	 */
	public void setHorizontalScrollPixel(int scrollIndex) {
		scrollIndex= Math.max(0, scrollIndex);
		fHorizontalScrollPixel= scrollIndex;
	}
}
