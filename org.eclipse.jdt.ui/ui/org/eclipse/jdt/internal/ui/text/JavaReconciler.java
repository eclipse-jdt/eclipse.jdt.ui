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

package org.eclipse.jdt.internal.ui.text;


import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.internal.ui.text.java.JavaReconcilingStrategy;


 
/**
 * A reconciler that is also activated on editor activation.
 */
public class JavaReconciler extends MonoReconciler {
	
	/**
	 * Internal part listener for activating the reconciler.
	 */
	class PartListener implements IPartListener {
		
		/*
		 * @see IPartListener#partActivated(IWorkbenchPart)
		 */
		public void partActivated(IWorkbenchPart part) {
			if (part == fTextEditor)
				JavaReconciler.this.forceReconciling();
		}

		/*
		 * @see IPartListener#partBroughtToTop(IWorkbenchPart)
		 */
		public void partBroughtToTop(IWorkbenchPart part) {
		}

		/*
		 * @see IPartListener#partClosed(IWorkbenchPart)
		 */
		public void partClosed(IWorkbenchPart part) {
		}

		/*
		 * @see IPartListener#partDeactivated(IWorkbenchPart)
		 */
		public void partDeactivated(IWorkbenchPart part) {
		}

		/*
		 * @see IPartListener#partOpened(IWorkbenchPart)
		 */
		public void partOpened(IWorkbenchPart part) {
		}
	};
	
	
	/** The reconciler's editor */
	private ITextEditor fTextEditor;
	/** The part listener */
	private IPartListener fPartListener;
	
	
	/**
	 * Creates a new reconciler.
	 */
	public JavaReconciler(ITextEditor editor, IReconcilingStrategy strategy, boolean isIncremental) {
		super(strategy, isIncremental);
		fTextEditor= editor;
	}
	
	/*
	 * @see IReconciler#install(ITextViewer)
	 */
	public void install(ITextViewer textViewer) {
		super.install(textViewer);
		
		fPartListener= new PartListener();
		IWorkbenchPartSite site= fTextEditor.getSite();
		IWorkbenchWindow window= site.getWorkbenchWindow();
		window.getPartService().addPartListener(fPartListener);
	}

	/*
	 * @see IReconciler#uninstall()
	 */
	public void uninstall() {
		
		IWorkbenchPartSite site= fTextEditor.getSite();
		IWorkbenchWindow window= site.getWorkbenchWindow();
		window.getPartService().removePartListener(fPartListener);
		fPartListener= null;
		
		super.uninstall();
	}
	
    /*
	 * @see AbstractReconciler#forceReconciling()
	 */
	protected void forceReconciling() {
		super.forceReconciling();
        IReconcilingStrategy strategy= getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
        if (strategy instanceof JavaReconcilingStrategy) {
			JavaReconcilingStrategy java= (JavaReconcilingStrategy) strategy;
			java.notifyParticipants(false);
		}
	}
    
	/*
	 * @see AbstractReconciler#reconcilerReset()
	 */
	protected void reconcilerReset() {
		super.reconcilerReset();
        IReconcilingStrategy strategy= getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
        if (strategy instanceof JavaReconcilingStrategy) {
			JavaReconcilingStrategy java= (JavaReconcilingStrategy) strategy;
			java.notifyParticipants(true);
		}
	}
}
