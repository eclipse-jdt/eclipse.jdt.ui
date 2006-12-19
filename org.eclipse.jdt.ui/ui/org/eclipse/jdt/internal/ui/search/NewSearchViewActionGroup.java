/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.OpenEvent;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.ui.actions.GenerateActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.OpenEditorActionGroup;
import org.eclipse.jdt.ui.actions.OpenViewActionGroup;
import org.eclipse.jdt.ui.actions.RefactorActionGroup;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;

class NewSearchViewActionGroup extends CompositeActionGroup {
	private OpenEditorActionGroup fOpenEditorActionGroup;
	
	public NewSearchViewActionGroup(IViewPart part) {
		Assert.isNotNull(part);
		OpenViewActionGroup openViewActionGroup;
		setGroups(new ActionGroup[]{
			fOpenEditorActionGroup= new OpenEditorActionGroup(part),
			openViewActionGroup= new OpenViewActionGroup(part),
			new GenerateActionGroup(part), 
			new RefactorActionGroup(part),
			new JavaSearchActionGroup(part) 
		});
		openViewActionGroup.containsShowInMenu(false);
	}
	
	public void handleOpen(OpenEvent event) {
		IAction openAction= fOpenEditorActionGroup.getOpenAction();
		if (openAction != null && openAction.isEnabled()) {
			openAction.run();
			return;
		}
	}
}

