/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.browsing;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
 
/**
 * This action selects all entries currently showing in view.
 */
class SelectAllAction extends Action implements ISelectionChangedListener, ISelectionListener {

	private TableViewer fViewer;

	/**
	 * Creates the action.
	 */
	SelectAllAction(TableViewer viewer) {
		super("selectAll"); //$NON-NLS-1$
		setText(JavaBrowsingMessages.getString("SelectAllAction.label")); //$NON-NLS-1$
		setToolTipText(JavaBrowsingMessages.getString("SelectAllAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.SELECT_ALL_ACTION);
		Assert.isNotNull(viewer);
		fViewer= viewer;
		updateEnablement();
	}

	/**
	 * Selects all resources in the view.
	 */
	public void run() {
		fViewer.getTable().selectAll();
		// force viewer selection change
		fViewer.setSelection(fViewer.getSelection());
	}

	/* (non-Javadoc)
	 * Method declared on ISelectionChangedListener.
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		updateEnablement();
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		updateEnablement();
	}

	private void updateEnablement() {
		boolean enabled= fViewer.getTable().getItemCount() > 0;
		setEnabled(enabled);
	}
}