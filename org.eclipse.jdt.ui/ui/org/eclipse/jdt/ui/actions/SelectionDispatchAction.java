/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.texteditor.IUpdate;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.actions.ActionUtil;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public abstract class SelectionDispatchAction extends Action implements ISelectionChangedListener, IUpdate {
	
	private UnifiedSite fSite;
	
	/**
	 * Creates a new action with no text and no image.
	 * <p>
	 * Configure the action later using the set methods.
	 * </p>
	 * 
	 * @param site the site this action is working on
	 */
	protected SelectionDispatchAction(UnifiedSite site) {
		Assert.isNotNull(site);
		fSite= site;
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		selectionChanged((ISelection)selection);
	}
	
	protected void selectionChanged(ITextSelection selection) {
		selectionChanged((ISelection)selection);
	}
	
	protected void selectionChanged(ISelection selection) {
		setEnabled(false);
	}
	
	protected void run(IStructuredSelection selection) throws CoreException {
		run((ISelection)selection);
	}
	
	protected void run(ITextSelection selection) throws CoreException {
		run((ISelection)selection);
	}
	
	protected void run(ISelection selection) throws CoreException {
	}

	protected UnifiedSite getSite() {
		return fSite;
	}
	
	protected  Shell getShell() {
		return fSite.getShell();
	}
	
	protected ISelectionProvider getSelectionProvider() {
		return fSite.getSelectionProvider();
	}

	protected ISelection getSelection() {
		return getSelectionProvider().getSelection();
	}

	/* (non-Javadoc)
	 * Method declared on IAction.
	 */
	public void run() {
		dispatchRun(getSelection());
	}
	
	/* (non-Javadoc)
	 * Method declared on ISelectionChangedListener.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		dispatchSelectionChanged(event.getSelection());
	}
	
	/* (non-Javadoc)
	 * Method declared on IUpdate
	 */
	public void update() {
		update(getSelection());
	}
	
	/**
	 * Updates the action's enablement state according to the given selection.
	 * 
	 * @param selection the selection this action is working on
	 */
	public void update(ISelection selection) {
		dispatchSelectionChanged(selection);
	}
	
	private void dispatchSelectionChanged(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			selectionChanged((IStructuredSelection)selection);
		} else if (selection instanceof ITextSelection) {
			selectionChanged((ITextSelection)selection);
		} else {
			selectionChanged(selection);
		}
	}

	private void dispatchRun(ISelection selection) {
		try {
			if (selection instanceof IStructuredSelection) {
				run((IStructuredSelection)selection);
			} else if (selection instanceof ITextSelection) {
				run((ITextSelection)selection);
			} else {
				run(selection);
			}
		} catch (CoreException e) {
			ExceptionHandler.handle(e, getShell(), ActionUtil.getText(this), ActionMessages.getString("SelectionDispatchAction.unexpected_exception")); //$NON-NLS-1$
		}
	}
}