package org.eclipse.jdt.internal.ui.text;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */


import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;


 
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
}
