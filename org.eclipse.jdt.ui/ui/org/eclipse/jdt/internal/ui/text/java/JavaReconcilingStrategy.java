/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.java;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.IWorkingCopyManager;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.preferences.WorkInProgressPreferencePage;



public class JavaReconcilingStrategy implements IReconcilingStrategy {
	
	private ICompilationUnit fUnit;
	private IEditorPart fEditor;
	private JavaEditor fJavaEditor;
	private IWorkingCopyManager fManager;
	
	public JavaReconcilingStrategy(IEditorPart editor) {
		
		fEditor= editor;
		fManager= JavaPlugin.getDefault().getWorkingCopyManager();
		
		if (!WorkInProgressPreferencePage.synchronizeOutlineOnCursorMove()) {
			if (fEditor instanceof JavaEditor)
				fJavaEditor= (JavaEditor) fEditor;
		}
	}
	
	private void reconcile() {
		ICompilationUnit unit= fManager.getWorkingCopy(fEditor.getEditorInput());		
		if (unit != null) {
			synchronized (unit) {
				try {
					
					// reconcile
					unit.reconcile();
					
					// update selection
					if (fJavaEditor != null) {
						Shell shell= fJavaEditor.getSite().getShell();
						if (shell != null && !shell.isDisposed()) {
							shell.getDisplay().asyncExec(new Runnable() {
								public void run() {
									fJavaEditor.synchronizeOutlinePageSelection();
								}
							});
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