/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;

public class WorkingSetAction extends ElementSearchAction {

	private ElementSearchAction fAction;

	public WorkingSetAction(ElementSearchAction action, String workingSetName) {
		super(workingSetName, null);
		Assert.isNotNull(action);
		fAction= action;
	}

	public void run() {
		fAction.run();
	}

	public boolean canOperateOn(ISelection sel) {
		return fAction.canOperateOn(sel);
	}

	protected int getLimitTo() {
		return -1;
	}
}
