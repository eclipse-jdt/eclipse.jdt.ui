package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;

import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;



public class JavaReconcilingStrategy implements IReconcilingStrategy {
	
	
	private ITextEditor fEditor;
	
	private IWorkingCopyManager fManager;
	private ReconcilingProblemRequestor fProblemRequestor;
	private ICompilationUnit fUnit;
	
	
	public JavaReconcilingStrategy(ITextEditor editor) {
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		fProblemRequestor = new ReconcilingProblemRequestor(fEditor);
	}
	
	private void reconcile() {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());		
		if (unit != null) {
			synchronized (unit) {
				try {
					
					// reconcile
					fProblemRequestor.init();
					unit.reconcile(fProblemRequestor);
					
					if (fEditor instanceof IProblemAcceptor) {
						IProblemAcceptor a= (IProblemAcceptor) fEditor;
						a.setProblems(fProblemRequestor.getProblems());
					}
					
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
}