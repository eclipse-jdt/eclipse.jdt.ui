/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;
import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.NavigateActionGroup;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;

class NewSearchViewActionGroup extends CompositeActionGroup {
	JavaSearchActionGroup fJavaSearchActionGroup;
	NavigateActionGroup fNavigateActionGroup;
	
	public NewSearchViewActionGroup(IViewPart part) {
		Assert.isNotNull(part);
		fJavaSearchActionGroup= new JavaSearchActionGroup(part);
		fNavigateActionGroup= new NavigateActionGroup(part);
		setGroups(new ActionGroup[]{fJavaSearchActionGroup, fNavigateActionGroup});
	}
	
	public void handleOpen(OpenEvent event) {
		IAction openAction= fNavigateActionGroup.getOpenAction();
		if (openAction != null && openAction.isEnabled()) {
			openAction.run();
			return;
		}
	}
}

