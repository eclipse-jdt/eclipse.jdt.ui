package org.eclipse.jdt.internal.ui.text.java;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IProblemRequestor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.IProblem;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;



public class JavaReconcilingStrategy implements IReconcilingStrategy {
	
	
	/**
	 * Internal problem requestor.
	 */
	static class ProblemRequestor implements IProblemRequestor {
		
		private List fProblems= new ArrayList();
		
		/*
		 * @see IProblemRequestor#acceptProblem(int, int, int, int, String, int)
		 */
		public void acceptProblem(IProblem problem) {
			fProblems.add(problem);
		}
		
		public List getProblems() {
			return new ArrayList(fProblems);
		}
		
		public void clear() {
			fProblems.clear();
		}
	};
	
	
	private ICompilationUnit fUnit;
	private IEditorPart fEditor;
	private CompilationUnitEditor fCompilationUnitEditor;
	private IWorkingCopyManager fManager;
	private ProblemRequestor fProblemRequestor= new ProblemRequestor();
	
	public JavaReconcilingStrategy(IEditorPart editor) {
		
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		
		if (fEditor instanceof CompilationUnitEditor)
			fCompilationUnitEditor= (CompilationUnitEditor) fEditor;
	}
	
	private void reconcile() {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());		
		if (unit != null) {
			synchronized (unit) {
				try {
					
					// reconcile
					fProblemRequestor.clear();
					unit.reconcile(fProblemRequestor);
					
					if (fCompilationUnitEditor != null) {
						
						fCompilationUnitEditor.setProblems(fProblemRequestor.getProblems());
						
						if (WorkInProgressPreferencePage.synchronizeOutlineOnCursorMove()) {
							Shell shell= fCompilationUnitEditor.getSite().getShell();
							if (shell != null && !shell.isDisposed()) {
								shell.getDisplay().asyncExec(new Runnable() {
									public void run() {
										fCompilationUnitEditor.synchronizeOutlinePageSelection();
									}
								});
							}
						}
					}
						
				} catch (JavaModelException x) {
					// ignored
				}
			}
		}
	}
	
	/**
	 * @see IReconcilingStrategy#reconcile(IRegion)
	 */
	public void reconcile(IRegion partition) {
		reconcile();
	}
	
	/**
	 * @see IReconcilingStrategy#reconcile(DirtyRegion, IRegion)
	 */
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		reconcile();
	}
	
	/**
	 * @see IReconcilingStrategy#setDocument(IDocument)
	 */
	public void setDocument(IDocument document) {
	}
}