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

package org.eclipse.jdt.internal.ui.text.comment;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;

/**
 * Text measurement based on the font set for a SWT control.
 * 
 * @since 3.0
 */
public class DefaultTextMeasurement implements ITextMeasurement {
	
	/** Control */
	private Control fControl;
	
	/**
	 * Initialize with the given control.
	 * 
	 * @param control The control
	 */
	public DefaultTextMeasurement(Control control) {
		fControl= control;
	}
	
	/*
	 * @see org.eclipse.jdt.internal.ui.text.comment.ITextMeasurement#computeWidth(java.lang.String)
	 */
	public int computeWidth(String string) {
		GC graphics= new GC(fControl);
		graphics.setFont(fControl.getFont());
		int width= graphics.stringExtent(string).x;
		graphics.dispose();
		return width;
	}
}
