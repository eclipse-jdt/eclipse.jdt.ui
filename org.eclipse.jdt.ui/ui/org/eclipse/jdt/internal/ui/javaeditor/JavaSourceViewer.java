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

package org.eclipse.jdt.internal.ui.javaeditor;

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.text.formatter.FormattingContextProperties;
import org.eclipse.jface.text.formatter.IFormattingContext;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.ui.text.comment.CommentFormattingContext;



public class JavaSourceViewer extends SourceViewer {

	/**
	 * Text operation code for requesting the outline for the current input.
	 */
	public static final int SHOW_OUTLINE= 51;

	/**
	 * Text operation code for requesting the outline for the element at the current position.
	 */
	public static final int OPEN_STRUCTURE= 52;

	/**
	 * Text operation code for requesting the hierarchy for the current input.
	 */
	public static final int SHOW_HIERARCHY= 53;

	private IInformationPresenter fOutlinePresenter;
	private IInformationPresenter fStructurePresenter;
	private IInformationPresenter fHierarchyPresenter;

	public JavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean showAnnotationsOverview, int styles) {
		super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles);
	}

	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#createFormattingContext()
	 * @since 3.0
	 */
	public IFormattingContext createFormattingContext() {

		final IFormattingContext context= new CommentFormattingContext();
		if (context != null) {
			
			final Map map= new Hashtable(JavaCore.getOptions());
			
			context.storeToMap(PreferenceConstants.getPreferenceStore(), map, false);
			context.setProperty(FormattingContextProperties.CONTEXT_PREFERENCES, map);
		}
		return context;
	}

	/*
	 * @see ITextOperationTarget#doOperation(int)
	 */
	public void doOperation(int operation) {
		if (getTextWidget() == null)
			return;

		switch (operation) {
			case SHOW_OUTLINE:
				fOutlinePresenter.showInformation();
				return;
			case OPEN_STRUCTURE:
				fStructurePresenter.showInformation();
				return;
			case SHOW_HIERARCHY:
				fHierarchyPresenter.showInformation();
				return;	
		}
		
		super.doOperation(operation);
	}

	/*
	 * @see ITextOperationTarget#canDoOperation(int)
	 */
	public boolean canDoOperation(int operation) {
		if (operation == SHOW_OUTLINE)
			return fOutlinePresenter != null;
		if (operation == OPEN_STRUCTURE)
			return fStructurePresenter != null;
		if (operation == SHOW_HIERARCHY)
			return fHierarchyPresenter != null;			
			
		return super.canDoOperation(operation);
	}

	/*
	 * @see ISourceViewer#configure(SourceViewerConfiguration)
	 */
	public void configure(SourceViewerConfiguration configuration) {
		super.configure(configuration);
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fOutlinePresenter= ((JavaSourceViewerConfiguration)configuration).getOutlinePresenter(this, false);
			fOutlinePresenter.install(this);
		}
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fStructurePresenter= ((JavaSourceViewerConfiguration)configuration).getOutlinePresenter(this, true);
			fStructurePresenter.install(this);
		}
		if (configuration instanceof JavaSourceViewerConfiguration) {
			fHierarchyPresenter= ((JavaSourceViewerConfiguration)configuration).getHierarchyPresenter(this, true);
			fHierarchyPresenter.install(this);
		}
	}

	/*
	 * @see TextViewer#handleDispose()
	 */
	protected void handleDispose() {
		if (fOutlinePresenter != null) {
			fOutlinePresenter.uninstall();	
			fOutlinePresenter= null;
		}
		if (fStructurePresenter != null) {
			fStructurePresenter.uninstall();
			fStructurePresenter= null;
		}
		if (fHierarchyPresenter != null) {
			fHierarchyPresenter.uninstall();
			fHierarchyPresenter= null;
		}		
		super.handleDispose();
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#rememberSelection()
	 */
	public Point rememberSelection() {
		return super.rememberSelection();
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewer#restoreSelection()
	 */
	public void restoreSelection() {
		super.restoreSelection();
	}
}
