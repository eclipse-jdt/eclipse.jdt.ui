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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

import org.eclipse.jdt.core.ISourceRange;

import org.eclipse.jdt.ui.PreferenceConstants;


public abstract class SourceContextViewer  implements IStatusContextViewer {

	private SourceViewer fSourceViewer;

	protected SourceViewer getSourceViewer() {
		return fSourceViewer;
	}
	
	//---- Methods defined in IStatusContextViewer -------------------------------

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IStatusContextViewer#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fSourceViewer= new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IStatusContextViewer#getControl()
	 */
	public Control getControl() {
		return fSourceViewer.getControl();
	}

	//---- Helper methods to populate viewer -------------------------------

	protected IDocument getDocument(IDocumentProvider provider, IEditorInput input) {
		if (input == null)
			return null;
		IDocument result= null;
		try {
			provider.connect(input);
			result= provider.getDocument(input);
		} catch (CoreException e) {
		} finally {
			provider.disconnect(input);
		}
		return result;
	}

	protected void setInput(IDocument document, ISourceRange range) {
		Control ctrl= getControl();
		if (ctrl != null && ctrl.isDisposed())
			ctrl= null;
		try {
			if (ctrl != null)
				ctrl.setRedraw(false);
			fSourceViewer.setInput(document);
			if (range != null) {
				int offset= range.getOffset();
				int length= range.getLength();
				if (offset >= 0 && length >= 0) {
					fSourceViewer.setSelectedRange(offset, length);
					fSourceViewer.revealRange(offset, length);
				}
			}
		} finally {
			if (ctrl != null)
				ctrl.setRedraw(true);
		}
	}
}

