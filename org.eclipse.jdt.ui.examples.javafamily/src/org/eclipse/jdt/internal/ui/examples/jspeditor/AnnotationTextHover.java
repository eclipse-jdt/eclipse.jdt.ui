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

package org.eclipse.jdt.internal.ui.examples.jspeditor;

import java.util.Iterator;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

/**
 * A simple text hover to show annotation information.
 * 
 * @since 3.0
 */
public class AnnotationTextHover implements ITextHover {

	/** This hover's annotation model */
	private IAnnotationModel fModel;

	/**
	 * Creates a new annotation hover.
	 * 
	 * @param model this hover's annotation model
	 */
	public AnnotationTextHover(IAnnotationModel model)  {
		Assert.isNotNull(model);
		fModel= model;
	}
	
	/*
	 * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		
		Iterator e= fModel.getAnnotationIterator();
		while (e.hasNext()) {
			Annotation a= (Annotation) e.next();
			Position p= fModel.getPosition(a);
			if (p != null && p.overlapsWith(hoverRegion.getOffset(), hoverRegion.getLength())) {
				String msg= a.getText();
				if (msg != null && msg.trim().length() > 0)
					return msg;
			}
		}
		
		return null;
	}

	/*
	 * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		/*
		 * XXX: If this is too slow then we might return new Region(offset, 0);
		 */ 
		Iterator e= fModel.getAnnotationIterator();
		while (e.hasNext()) {
			Annotation a= (Annotation) e.next();
			Position p= fModel.getPosition(a);
			if (p.overlapsWith(offset, 0)) {
				String msg= a.getText();
				if (msg != null && msg.trim().length() > 0)
					return new Region(p.offset, p.length);
			}
		}
		return null;
	}
}
