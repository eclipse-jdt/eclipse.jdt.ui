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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.reconciler.AbstractReconciler;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.IAnnotationModel;

import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.ITextEditor;

import org.eclipse.jdt.core.IProblemRequestor;

import org.eclipse.jdt.internal.ui.text.java.JavaReconcilingStrategy;

/**
 * A multi-pass reconciler that is also activated on editor activation.
 * <p>
 * This reconciler behaves like a mono reconciler, except that it allows for
 * multiple passes over the dirty region.
 * <p>
 * Reconciling strategies can be registered by calling <code>addReconcilingStrategy(IReconcilingStrategy, String)</code>.
 * The order of calls to register the strategies determines the order in which
 * they are executed during the multi-pass reconciling.
 * 
 * @since 3.0
 */
public class JavaMultiPassReconciler extends AbstractReconciler {

	/**
	 * Internal shell activation listener for activating the reconciler.
	 */
	protected class ActivationListener extends ShellAdapter {

		/** The control being activated */
		private Control fControl;

		/**
		 * Creates a new shell activation listener
		 * 
		 * @param control
		 *                  The control to listen for its activations
		 */
		public ActivationListener(Control control) {
			fControl= control;
		}

		/*
		 * @see org.eclipse.swt.events.ShellAdapter#shellActivated(org.eclipse.swt.events.ShellEvent)
		 */
		public void shellActivated(ShellEvent event) {

			if (!fControl.isDisposed() && fControl.isVisible())
				JavaMultiPassReconciler.this.forceReconciling();
		}
	}

	/**
	 * Internal part listener for activating the reconciler.
	 */
	protected class PartListener implements IPartListener {

		/*
		 * @see IPartListener#partActivated(IWorkbenchPart)
		 */
		public void partActivated(IWorkbenchPart part) {
			if (part == fEditor)
				JavaMultiPassReconciler.this.forceReconciling();
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
	}

	/** The shell listener */
	private ShellListener fActivationListener;

	/** The text editor to operate on */
	private final ITextEditor fEditor;

	/** The part listener */
	private IPartListener fPartListener;

	/** The problem requestor */
	private final IProblemRequestor fRequestor;

	/** The list of strategies */
	private final List fStrategies= new LinkedList();

	/** The map from types to strategies */
	private final Map fTypes= new HashMap();

	/**
	 * Creates a new compilation unit reconciler.
	 * 
	 * @param editor
	 *                  The text editor to operate on
	 */
	public JavaMultiPassReconciler(ITextEditor editor) {
		fEditor= editor;

		final IAnnotationModel model= editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		Assert.isLegal(model instanceof IProblemRequestor);

		fRequestor= (IProblemRequestor)model;
	}

	/**
	 * Adds a new reconciling strategy to the multi-pass reconciler.
	 * <p>
	 * If there is already a strategy registered with the content type, nothing
	 * happens. If not, the strategy is registered with the specified type and
	 * appended to the strategies list
	 * 
	 * @param strategy
	 *                  Strategy to append to the multi-pass reconciling process
	 * @param type
	 *                  The type to register the strategy with
	 */
	public void addReconcilingStrategy(IReconcilingStrategy strategy, String type) {

		if (!fTypes.containsKey(type)) {

			fTypes.put(type, strategy);

			if (!fStrategies.contains(strategy))
				fStrategies.add(strategy);
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#forceReconciling()
	 */
	protected void forceReconciling() {

		super.forceReconciling();

		final IReconcilingStrategy strategy= getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
		if (strategy != null && strategy instanceof JavaReconcilingStrategy) {

			JavaReconcilingStrategy java= (JavaReconcilingStrategy)strategy;
			java.notifyParticipants(false);
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconciler#getReconcilingStrategy(java.lang.String)
	 */
	public IReconcilingStrategy getReconcilingStrategy(String type) {
		return (IReconcilingStrategy)fTypes.get(type);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#initialProcess()
	 */
	protected void initialProcess() {

		IReconcilingStrategy strategy= null;
		IReconcilingStrategyExtension extension= null;

		for (final Iterator iterator= fStrategies.iterator(); iterator.hasNext();) {

			strategy= (IReconcilingStrategy)iterator.next();
			if (strategy instanceof IReconcilingStrategyExtension) {

				extension= (IReconcilingStrategyExtension)strategy;
				extension.initialReconcile();
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconciler#install(org.eclipse.jface.text.ITextViewer)
	 */
	public void install(ITextViewer textViewer) {

		super.install(textViewer);

		fPartListener= new PartListener();

		final IWorkbenchWindow window= fEditor.getSite().getWorkbenchWindow();
		window.getPartService().addPartListener(fPartListener);

		fActivationListener= new ActivationListener(textViewer.getTextWidget());
		window.getShell().addShellListener(fActivationListener);
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#process(org.eclipse.jface.text.reconciler.DirtyRegion)
	 */
	protected void process(DirtyRegion region) {

		try {
			fRequestor.beginReporting();

			IReconcilingStrategy strategy= null;
			for (final Iterator iterator= fStrategies.iterator(); iterator.hasNext();) {

				strategy= (IReconcilingStrategy)iterator.next();
				process(strategy, region);
			}
		} finally {
			fRequestor.endReporting();
		}
	}

	/**
	 * Processes a dirty region. If the dirty region is <code>null</code> the
	 * whole document is consider being dirty. The dirty region is partitioned
	 * by the document and each partition is handed over to a reconciling
	 * strategy registered for the partition's content type.
	 * 
	 * @param strategy
	 *                  The reconciling strategy to use
	 * @param region
	 *                  The dirty region to be processed
	 */
	protected void process(IReconcilingStrategy strategy, DirtyRegion region) {

		if (region != null)
			strategy.reconcile(region, region);
		else {
			IDocument document= getDocument();
			if (document != null)
				strategy.reconcile(new Region(0, document.getLength()));
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#reconcilerDocumentChanged(org.eclipse.jface.text.IDocument)
	 */
	protected void reconcilerDocumentChanged(IDocument document) {

		IReconcilingStrategy strategy= null;
		for (final Iterator iterator= fStrategies.iterator(); iterator.hasNext();) {

			strategy= (IReconcilingStrategy)iterator.next();
			strategy.setDocument(document);
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#reconcilerReset()
	 */
	protected void reconcilerReset() {

		super.reconcilerReset();

		final IReconcilingStrategy strategy= getReconcilingStrategy(IDocument.DEFAULT_CONTENT_TYPE);
		if (strategy != null && strategy instanceof JavaReconcilingStrategy) {

			JavaReconcilingStrategy java= (JavaReconcilingStrategy)strategy;
			java.notifyParticipants(true);
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.AbstractReconciler#setProgressMonitor(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void setProgressMonitor(IProgressMonitor monitor) {

		super.setProgressMonitor(monitor);

		IReconcilingStrategy strategy= null;
		IReconcilingStrategyExtension extension= null;

		for (final Iterator iterator= fStrategies.iterator(); iterator.hasNext();) {

			strategy= (IReconcilingStrategy)iterator.next();
			if (strategy instanceof IReconcilingStrategyExtension) {

				extension= (IReconcilingStrategyExtension)strategy;
				extension.setProgressMonitor(monitor);
			}
		}
	}

	/*
	 * @see org.eclipse.jface.text.reconciler.IReconciler#uninstall()
	 */
	public void uninstall() {

		try {
			final IWorkbenchWindow window= fEditor.getSite().getWorkbenchWindow();

			window.getPartService().removePartListener(fPartListener);
			fPartListener= null;

			final Shell shell= window.getShell();
			if (shell != null && !shell.isDisposed())
				shell.removeShellListener(fActivationListener);

			fActivationListener= null;
		} finally {
			super.uninstall();
		}
	}
}
