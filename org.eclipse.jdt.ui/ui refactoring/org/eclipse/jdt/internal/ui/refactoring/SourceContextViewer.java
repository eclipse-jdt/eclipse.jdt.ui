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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.ui.PreferenceConstants;

class SourceContextViewer  extends SourceViewer implements IErrorContextViewer {

	public static class SourceContextInput {
		public IDocument document;
		public SourceViewerConfiguration configuration;
		public ISourceRange sourceRange;
		public SourceContextInput(IDocument d, SourceViewerConfiguration c, ISourceRange r) {
			document= d;
			configuration= c;
			sourceRange= r;
		}
	}

	SourceContextViewer(Composite parent) {
		super(parent, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		setEditable(false);
		getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
	}

	/* non Java-doc
	 * @see IErrorContextViewer
	 */	
	public void setInput(Object input) {
		if (input instanceof SourceContextInput) {
			SourceContextInput scinput= (SourceContextInput)input;
			configure(scinput.configuration);
			Control ctrl= getControl();
			if (ctrl != null && ctrl.isDisposed())
				ctrl= null;
			try {
				if (ctrl != null)
					ctrl.setRedraw(false);
				super.setInput(scinput.document);
				if (scinput.sourceRange != null) {
					int offset= scinput.sourceRange.getOffset();
					int length= scinput.sourceRange.getLength();
					if (offset >= 0 && length >= 0) {
						setSelectedRange(offset, length);
						revealRange(offset, length);
					}
				}
			} finally {
				if (ctrl != null)
					ctrl.setRedraw(true);
			}
		} else {
			super.setInput(input);
		}
	}	
}

