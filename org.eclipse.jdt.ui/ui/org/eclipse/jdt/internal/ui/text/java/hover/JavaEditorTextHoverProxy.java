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

package org.eclipse.jdt.internal.ui.text.java.hover;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;


/**
 * Proxy for JavaEditorTextHovers.
 * 
 * @since 2.1
 */
public class JavaEditorTextHoverProxy extends AbstractJavaEditorTextHover {

	private JavaEditorTextHoverDescriptor fHoverDescriptor;
	private IJavaEditorTextHover fHover;

	public JavaEditorTextHoverProxy(JavaEditorTextHoverDescriptor descriptor, IEditorPart editor) {
		fHoverDescriptor= descriptor;
		setEditor(editor);
	}

	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		super.setEditor(editor);

		if (fHover != null)
			fHover.setEditor(getEditor());
	}

	public boolean isEnabled() {
		return true;
	}

	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		if (!isEnabled() || fHoverDescriptor == null)
			return null;

		if (isCreated() || createHover())
			return fHover.getHoverRegion(textViewer, offset);
		else
			return null;
	}
	
	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		if (!isEnabled() || fHoverDescriptor == null)
			return null;

		if (isCreated() || createHover())
			return fHover.getHoverInfo(textViewer, hoverRegion);
		else
			return null;
	}

	private boolean isCreated() {
		return fHover != null;
	}

	private boolean createHover() {
		fHover= fHoverDescriptor.createTextHover();
		if (fHover != null)
			fHover.setEditor(getEditor());
		return isCreated();
	}
}
