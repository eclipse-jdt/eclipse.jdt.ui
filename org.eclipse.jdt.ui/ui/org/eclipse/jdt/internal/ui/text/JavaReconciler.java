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


import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.reconciler.MonoReconciler;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.JavaCore;


 
/**
 * A reconciler that is also activated on editor activation.
 */
public class JavaReconciler extends MonoReconciler {
	
	/**
	 * Internal part listener for activating the reconciler.
	 */
	private class PartListener implements IPartListener {
		
		/**
		 * {@inheritDoc}
		 */
		public void partActivated(IWorkbenchPart part) {
			if (part == fTextEditor && hasJavaModelChanged())
				JavaReconciler.this.forceReconciling();
		}

		/**
		 * {@inheritDoc}
		 */
		public void partBroughtToTop(IWorkbenchPart part) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void partClosed(IWorkbenchPart part) {
		}

		/**
		 * {@inheritDoc}
		 */
		public void partDeactivated(IWorkbenchPart part) {
			if (part == fTextEditor)
				setJavaModelChanged(false);
		}

		/**
		 * {@inheritDoc}
		 */
		public void partOpened(IWorkbenchPart part) {
		}
	}
	
	/**
	 * Internal Shell activation listener for activating the reconciler.
	 */
	private class ActivationListener extends ShellAdapter {
		
		private Control fControl;
		
		public ActivationListener(Control control) {
			fControl= control;
		}

		/**
		 * {@inheritDoc}
		 */
		public void shellActivated(ShellEvent e) {
			if (!fControl.isDisposed() && fControl.isVisible() && hasJavaModelChanged())
				JavaReconciler.this.forceReconciling();
		}
		
		/**
		 * {@inheritDoc}
		 */
		public void shellDeactivated(ShellEvent e) {
			setJavaModelChanged(false);
		}
	}
	
	/**
	 * Internal Java element changed listener
	 * 
	 * @since 3.0
	 */
	private class ElementChangedListener implements IElementChangedListener {
		
		/**
		 * {@inheritDoc}
		 */
		public void elementChanged(ElementChangedEvent event) {
			setJavaModelChanged(true);
		}
	}
	
	
	/** The reconciler's editor */
	private ITextEditor fTextEditor;
	/** The part listener */
	private IPartListener fPartListener;
	/** The shell listener */
	private ShellListener fActivationListener;
	/**
	 * The Java element changed listener.
	 * @since 3.0
	 */
	private IElementChangedListener fJavaElementChangedListener;
	/**
	 * Tells whether the Java model sent out a changed event.
	 * @since 3.0
	 */
	private volatile boolean fHasJavaModelChanged= true;
	
	
	/**
	 * Creates a new reconciler.
	 */
	public JavaReconciler(ITextEditor editor, JavaCompositeReconcilingStrategy strategy, boolean isIncremental) {
		super(strategy, isIncremental);
		fTextEditor= editor;
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void install(ITextViewer textViewer) {
		super.install(textViewer);
		
		fPartListener= new PartListener();
		IWorkbenchPartSite site= fTextEditor.getSite();
		IWorkbenchWindow window= site.getWorkbenchWindow();
		window.getPartService().addPartListener(fPartListener);
		
		fActivationListener= new ActivationListener(textViewer.getTextWidget());
		Shell shell= window.getShell();
		shell.addShellListener(fActivationListener);
		
		fJavaElementChangedListener= new ElementChangedListener();
		JavaCore.addElementChangedListener(fJavaElementChangedListener);
	}

	/**
	 * {@inheritDoc}
	 */
	public void uninstall() {
		
		IWorkbenchPartSite site= fTextEditor.getSite();
		IWorkbenchWindow window= site.getWorkbenchWindow();
		window.getPartService().removePartListener(fPartListener);
		fPartListener= null;
		
		Shell shell= window.getShell();
		if (shell != null && !shell.isDisposed())
			shell.removeShellListener(fActivationListener);
		fActivationListener= null;
		
		JavaCore.removeElementChangedListener(fJavaElementChangedListener);
		fJavaElementChangedListener= null;
		
		super.uninstall();
	}
	
	/**
	 * {@inheritDoc}
	 */
	protected void forceReconciling() {
		super.forceReconciling();
        JavaCompositeReconcilingStrategy strategy= (JavaCompositeReconcilingStrategy) getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
		strategy.notifyParticipants(false);
	}
    
	/**
	 * {@inheritDoc}
	 */
	protected void reconcilerReset() {
		super.reconcilerReset();
        JavaCompositeReconcilingStrategy strategy= (JavaCompositeReconcilingStrategy) getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
		strategy.notifyParticipants(true);
	}
	
	/**
	 * Tells whether the Java Model has changed or not.
	 * 
	 * @return <code>true</code> iff the Java Model has changed
	 * @since 3.0
	 */
	private synchronized boolean hasJavaModelChanged() {
		return fHasJavaModelChanged;
	}
	
	/**
	 * Sets whether the Java Model has changed or not.
	 * 
	 * @param state <code>true</code> iff the java model has changed
	 * @since 3.0
	 */
	private synchronized void setJavaModelChanged(boolean state) {
		fHasJavaModelChanged= state;
	}
}
