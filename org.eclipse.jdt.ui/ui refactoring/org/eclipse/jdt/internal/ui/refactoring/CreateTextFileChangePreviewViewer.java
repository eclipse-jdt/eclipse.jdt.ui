/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.corext.refactoring.nls.changes.CreateTextFileChange;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ViewerPane;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.ui.refactoring.ChangePreviewViewerInput;
import org.eclipse.ltk.ui.refactoring.IChangePreviewViewer;


public class CreateTextFileChangePreviewViewer implements IChangePreviewViewer {

	private ViewerPane fPane;
	private SourceViewer fSourceViewer;

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		fPane= new ViewerPane(parent, SWT.BORDER | SWT.FLAT);
		
		fSourceViewer= new SourceViewer(fPane, null, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		fSourceViewer.setEditable(false);
		fSourceViewer.getControl().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		fPane.setContent(fSourceViewer.getControl());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#getControl()
	 */
	public Control getControl() {
		return fPane;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#setInput(java.lang.Object)
	 */
	public void setInput(ChangePreviewViewerInput input) {
		Change change= input.getChange();
		if (!(change instanceof CreateTextFileChange)) {
			fSourceViewer.setInput(null);
			fPane.setText(""); //$NON-NLS-1$
			return;
		}
		CreateTextFileChange textFileChange= (CreateTextFileChange)change;
		fPane.setText(textFileChange.getName());
		IDocument document= new Document(textFileChange.getPreview());
		// This is a temporary work around until we get the
		// source viewer registry.
		if ("java".equals(textFileChange.getTextType())) { //$NON-NLS-1$
			JavaTextTools textTools= JavaPlugin.getDefault().getJavaTextTools();
			textTools.setupJavaDocumentPartitioner(document);
			IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
			fSourceViewer.configure(new JavaSourceViewerConfiguration(textTools.getColorManager(), store, null, null));
		} else {
			fSourceViewer.configure(new SourceViewerConfiguration());
		}
		fSourceViewer.setInput(document);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.IChangePreviewViewer#refresh()
	 */
	public void refresh() {
		fSourceViewer.refresh();
	}
}
