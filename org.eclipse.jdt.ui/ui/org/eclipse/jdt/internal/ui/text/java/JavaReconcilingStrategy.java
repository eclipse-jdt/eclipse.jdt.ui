/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.internal.ui.text.java;


import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.internal.ui.JavaPlugin;



public class JavaReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
	
	
	private ITextEditor fEditor;
	
	private IWorkingCopyManager fManager;
	private IDocumentProvider fDocumentProvider;
	private IProgressMonitor fProgressMonitor;
	private boolean fNotify= true;
	
	
	public JavaReconcilingStrategy(ITextEditor editor) {
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		fDocumentProvider= JavaPlugin.getDefault().getCompilationUnitDocumentProvider();
	}
	
	private IProblemRequestorExtension getProblemRequestorExtension() {
		IAnnotationModel model= fDocumentProvider.getAnnotationModel(fEditor.getEditorInput());
		if (model instanceof IProblemRequestorExtension)
			return (IProblemRequestorExtension) model;
		return null;
	}
	
	private void reconcile() {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());		
		if (unit != null) {
			try {
								
				/* fix for missing cancel flag communication */
				IProblemRequestorExtension extension= getProblemRequestorExtension();
				if (extension != null)
					extension.setProgressMonitor(fProgressMonitor);
				
				// reconcile
				JavaModelUtil.reconcile(unit, true, fProgressMonitor);
				
				/* fix for missing cancel flag communication */
				if (extension != null)
					extension.setProgressMonitor(null);
				
				// update participants
				try {
					if (fEditor instanceof IReconcilingParticipant && fNotify && !fProgressMonitor.isCanceled()) {
						IReconcilingParticipant p= (IReconcilingParticipant) fEditor;
						p.reconciled();
					}
				} finally {
					fNotify= true;
				}
				
			} catch (JavaModelException x) {
				// swallow exception
			}
		}
	}
	
	/*
	 * @see IReconcilingStrategy#reconcile(IRegion)
	 */
	public void reconcile(IRegion partition) {
		reconcile();
	}
	
	/*
	 * @see IReconcilingStrategy#reconcile(DirtyRegion, IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile();
	}
	
	/*
	 * @see IReconcilingStrategy#setDocument(IDocument)
	 */
	public void setDocument(IDocument document) {
	}
	
	/*
	 * @see IReconcilingStrategyExtension#setProgressMonitor(IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {
		fProgressMonitor= monitor;
	}

	/*
	 * @see IReconcilingStrategyExtension#initialReconcile()
	 */
	public void initialReconcile() {
		reconcile();
	}
	
	/**
	 * Tells this strategy whether to inform its participants.
	 * 
	 * @param notify <code>true</code> if participant should be notified
	 */
	public void notifyParticipants(boolean notify) {
		fNotify= notify;
	}
}