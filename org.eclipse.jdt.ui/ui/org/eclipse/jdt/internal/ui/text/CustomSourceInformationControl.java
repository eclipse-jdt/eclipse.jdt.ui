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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.resource.JFaceResources;

import org.eclipse.jface.text.ITextViewerExtension;

import org.eclipse.jdt.internal.ui.text.java.hover.SourceViewerInformationControl;

/**
 * CustomSourceInformationControl
 * @since 3.0
 */
public class CustomSourceInformationControl extends SourceViewerInformationControl {

	private static final String SYMBOLIC_FONT_NAME= "org.eclipse.jdt.ui.editors.textfont"; //$NON-NLS-1$
	
	int fMaxWidth= Integer.MAX_VALUE;
	int fMaxHeight= Integer.MAX_VALUE;
	
	/*
	 * @see org.eclipse.jface.text.IInformationControl#setSizeConstraints(int, int)
	 */
	public void setSizeConstraints(int maxWidth, int maxHeight) {
		fMaxWidth= maxWidth;
		fMaxHeight= maxHeight;
	}

	/**
	 * @param parent
	 */
	public CustomSourceInformationControl(Shell parent) {
		super(parent);
		setViewerFont();
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
}
