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

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.text.reconcilerpipe.DefaultAnnotationHover;

/**
 * Configuration information for a simple JSP source viewer.
 * 
 * @since 3.0
 */
public class JspSourceViewerConfiguration extends SourceViewerConfiguration {

	private ITextEditor fTextEditor;

	/**
	 * Creates a new JSP source viewer configuration that behaves
	 * according to the specification of this class' methods.
	 */
	public JspSourceViewerConfiguration(ITextEditor textEditor) {
		fTextEditor= textEditor;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		IReconcilingStrategy reconcilingStrategy= new JspReconcilingStrategy(sourceViewer, fTextEditor);
		MonoReconciler reconciler= new MonoReconciler(reconcilingStrategy, false);
		reconciler.setProgressMonitor(new NullProgressMonitor());		
		reconciler.setDelay(500);
		return reconciler;
	}

	/*
	 * @see SourceViewerConfiguration#getAnnotationHover(ISourceViewer)
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new DefaultAnnotationHover();
	}
}
