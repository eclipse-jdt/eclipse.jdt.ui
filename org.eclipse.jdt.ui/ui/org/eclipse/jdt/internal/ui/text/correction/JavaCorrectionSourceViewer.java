/*******************************************************************************
 * Copyright (c) 2000, 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

public class JavaCorrectionSourceViewer extends SourceViewer {

	/** 
	 * Text operation code for requesting correction assist to show correction
	 * proposals for the current position. 
	 */
	public static final int CORRECTIONASSIST_PROPOSALS= 50;

	/**
	 * Text operation code for requesting the outline for the current input.
	 */
	public static final int SHOW_OUTLINE= 51;

	/**
	 * Text operation code for requesting the outline for the element at the current position.
	 */
	public static final int OPEN_STRUCTURE= 52;

	private JavaCorrectionAssistant fCorrectionAssistant;
	private IInformationPresenter fOutlinePresenter;
	private IInformationPresenter fStructurePresenter;
	
	private IEditorPart fEditor;

	public JavaCorrectionSourceViewer(Composite parent, IVerticalRuler ruler, int styles, IEditorPart editor) {
		super(parent, ruler, styles);
		fEditor= editor;
	}

	/*
	 * @see ITextOperationTarget#doOperation(int)
	 */
	public void doOperation(int operation) {
		if (getTextWidget() == null)
			return;

		switch (operation) {
			case CORRECTIONASSIST_PROPOSALS:
				fCorrectionAssistant.showPossibleCompletions();
				return;
			case SHOW_OUTLINE:
				fOutlinePresenter.showInformation();
				return;
			case OPEN_STRUCTURE:
				fStructurePresenter.showInformation();
				return;
		}
		super.doOperation(operation);
	}

	/*
	 * @see ITextOperationTarget#canDoOperation(int)
	 */
	public boolean canDoOperation(int operation) {
		if (operation == CORRECTIONASSIST_PROPOSALS) {
			return true;
		}
		if (operation == SHOW_OUTLINE) {
			return fOutlinePresenter != null;
		}
		if (operation == OPEN_STRUCTURE) {
			return fStructurePresenter != null;
		}
		return super.canDoOperation(operation);
	}

	/*
	 * @see ISourceViewer#configure(SourceViewerConfiguration)
	 */
	public void configure(SourceViewerConfiguration configuration) {
		super.configure(configuration);
		fCorrectionAssistant= new JavaCorrectionAssistant(fEditor);
		fCorrectionAssistant.install(this);
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fOutlinePresenter= ((JavaSourceViewerConfiguration)configuration).getOutlinePresenter(this, false);
			fOutlinePresenter.install(this);
		}
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fStructurePresenter= ((JavaSourceViewerConfiguration)configuration).getOutlinePresenter(this, true);
			fStructurePresenter.install(this);
		}
	}

	/*
	 * @see TextViewer#handleDispose()
	 */
	protected void handleDispose() {
		if (fCorrectionAssistant != null) {
			fCorrectionAssistant.uninstall();
			fCorrectionAssistant= null;
		}
		if (fOutlinePresenter != null) {
			fOutlinePresenter.uninstall();	
			fOutlinePresenter= null;
		}
		if (fStructurePresenter != null) {
			fStructurePresenter.uninstall();
			fStructurePresenter= null;
		}
		super.handleDispose();
	}

};