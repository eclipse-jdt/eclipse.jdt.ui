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
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.actions.ActionGroup;

public class CompositeActionGroup extends ActionGroup {

	private ActionGroup[] fGroups;

	public CompositeActionGroup(ActionGroup[] groups) {
		Assert.isNotNull(groups);
		fGroups= groups;
	}

	public void dispose() {
		super.dispose();
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].dispose();
		}
	}

	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].fillActionBars(actionBars);
		}
	}

	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].fillContextMenu(menu);
		}
	}

	public ActionContext getContext() {
		return super.getContext();
	}

	public void setContext(ActionContext context) {
		super.setContext(context);
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].setContext(context);
		}
	}

	public void updateActionBars() {
		super.updateActionBars();
		for (int i= 0; i < fGroups.length; i++) {
			fGroups[i].updateActionBars();
		}
	}
}
