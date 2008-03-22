/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.java.hover;

import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHoverExtension;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.java.hover.IJavaEditorTextHover;


public class JavaTypeHover implements IJavaEditorTextHover, ITextHoverExtension, ITextHoverExtension2 {

	private AbstractJavaEditorTextHover fProblemHover;
	private AbstractJavaEditorTextHover fJavadocHover;

	/* @since 3.4 */
	private AbstractJavaEditorTextHover fCurrentHover;

	public JavaTypeHover() {
		fProblemHover= new ProblemHover();
		fJavadocHover= new JavadocHover();
		fCurrentHover= null;
	}

	/*
	 * @see IJavaEditorTextHover#setEditor(IEditorPart)
	 */
	public void setEditor(IEditorPart editor) {
		fProblemHover.setEditor(editor);
		fJavadocHover.setEditor(editor);
		fCurrentHover= null;
	}

	/*
	 * @see ITextHover#getHoverRegion(ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return fJavadocHover.getHoverRegion(textViewer, offset);
	}

	/*
	 * @see ITextHover#getHoverInfo(ITextViewer, IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return String.valueOf(getHoverInfo2(textViewer, hoverRegion));
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension2#getHoverInfo2(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 * @since 3.4
	 */
	public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
		Object hoverInfo= fProblemHover.getHoverInfo2(textViewer, hoverRegion);
		if (hoverInfo != null) {
			fCurrentHover= fProblemHover;
			return hoverInfo;
		}

		fCurrentHover= fJavadocHover;
		return fJavadocHover.getHoverInfo2(textViewer, hoverRegion);
	}

	/*
	 * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
	 * @since 3.4
	 */
	public IInformationControlCreator getHoverControlCreator() {
		return fCurrentHover == null ? null : fCurrentHover.getHoverControlCreator();
	}

	/*
	 * @see org.eclipse.jface.text.information.IInformationProviderExtension2#getInformationPresenterControlCreator()
	 * @since 3.4
	 */
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return fCurrentHover == null ? null : fCurrentHover.getInformationPresenterControlCreator();
	}
}
