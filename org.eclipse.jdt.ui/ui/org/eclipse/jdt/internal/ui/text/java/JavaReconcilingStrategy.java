package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.swt.widgets.Shell;

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
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.core.WorkingCopy;
import org.eclipse.jdt.internal.ui.JavaPlugin;



public class JavaReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {
	
	
	private ITextEditor fEditor;
	
	private IWorkingCopyManager fManager;
	private IDocumentProvider fDocumentProvider;
	private ICompilationUnit fUnit;
	private IProgressMonitor fProgressMonitor;
	
	
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
	
	private void reconcile(boolean forceProblemDetection) {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());		
		if (unit != null) {
			synchronized (unit) {
				try {
					
					// reconcile
					
					/* fix for missing cancel flag communication */
					IProblemRequestorExtension extension= getProblemRequestorExtension();
					if (extension != null)
						extension.setProgressMonitor(fProgressMonitor);
					
					unit.reconcile(forceProblemDetection, fProgressMonitor);
					
					/* fix for missing cancel flag communication */
					if (extension != null)
						extension.setProgressMonitor(null);
					
					if (fEditor instanceof IReconcilingParticipant) {
						IReconcilingParticipant p= (IReconcilingParticipant) fEditor;
						p.reconciled();
					}
					
				} catch (JavaModelException x) {
					// ignored
				}
			}
		}
	}
	
	/*
	 * @see IReconcilingStrategy#reconcile(IRegion)
	 */
	public void reconcile(IRegion partition) {
		reconcile(false);
	}
	
	/*
	 * @see IReconcilingStrategy#reconcile(DirtyRegion, IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile(false);
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
		reconcile(true);
	}
}