/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
 
/**
 * This action selects all entries currently showing in view.
 */
public class SelectAllAction extends Action {

	private StructuredViewer fViewer;

	/**
	 * Creates the action for a TreeViewer
	 */
	public SelectAllAction(TreeViewer viewer) {
		this((StructuredViewer) viewer);
	}
	
	/**
	 * Creates the action for a TableViewer
	 */
	public SelectAllAction(TableViewer viewer) {
		this((StructuredViewer) viewer);
	}

	private SelectAllAction(StructuredViewer viewer) {
		super("selectAll"); //$NON-NLS-1$
		setText(ActionMessages.SelectAllAction_label); 
		setToolTipText(ActionMessages.SelectAllAction_tooltip); 
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.SELECT_ALL_ACTION);
		Assert.isNotNull(viewer);
		fViewer= viewer;
	}

	/**
	 * Selects all resources in the view.
	 */
	public void run() {
		if (fViewer instanceof TreeViewer) {
			((TreeViewer) fViewer).getTree().selectAll();
		} else if (fViewer instanceof TableViewer) {
			((TableViewer) fViewer).getTable().selectAll();
		}
		// force viewer selection change
		fViewer.setSelection(fViewer.getSelection());
	}
}
