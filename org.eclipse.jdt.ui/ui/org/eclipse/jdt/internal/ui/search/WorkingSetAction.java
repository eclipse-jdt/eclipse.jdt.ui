/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class WorkingSetAction extends ElementSearchAction {

	private ElementSearchAction fAction;

	public WorkingSetAction(IWorkbenchSite site, ElementSearchAction action, String workingSetName) {
		super(site, workingSetName, null);
		Assert.isNotNull(action);
		fAction= action;
	}

	public WorkingSetAction(JavaEditor editor, ElementSearchAction action, String workingSetName) {
		super(editor, workingSetName, null);
		Assert.isNotNull(action);
		fAction= action;
	}

	public void run() {
		fAction.run();
	}

	public boolean canOperateOn(IStructuredSelection sel) {
		return fAction.canOperateOn(sel);
	}

	protected int getLimitTo() {
		return -1;
	}
}
