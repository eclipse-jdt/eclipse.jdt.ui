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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionGroup;

import org.eclipse.jdt.ui.actions.JavaSearchActionGroup;
import org.eclipse.jdt.ui.actions.NavigateActionGroup;

import org.eclipse.jdt.internal.ui.actions.CompositeActionGroup;

class SearchViewActionGroup extends CompositeActionGroup {

	public SearchViewActionGroup(IViewPart part) {
		Assert.isNotNull(part);
		setGroups(new ActionGroup[] {
			new JavaSearchActionGroup(part),
			new NavigateActionGroup(new SearchViewAdapter(new SearchViewSiteAdapter(part.getSite())))});
	}
}

