/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IJavaElement;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class WorkingSetAction extends JavaElementSearchAction {

	private JavaElementSearchAction fAction;

	public WorkingSetAction(IWorkbenchSite site, JavaElementSearchAction action, String workingSetName) {
		super(site, workingSetName, null);
		Assert.isNotNull(action);
		fAction= action;
	}

	public WorkingSetAction(JavaEditor editor, JavaElementSearchAction action, String workingSetName) {
		super(editor, workingSetName, null);
		Assert.isNotNull(action);
		fAction= action;
		setToolTipText(action.getToolTipText());
	}

	void run(IJavaElement element) {
		fAction.run(element);
	}

	boolean canOperateOn(IJavaElement element) {
		return fAction.canOperateOn(element);
	}

	int getLimitTo() {
		return -1;
	}

	String getOperationUnavailableMessage() {
		return fAction.getOperationUnavailableMessage();
	}

}
