/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *   Michael Fraenkel (fraenkel@us.ibm.com) - patch
 *          (report 60714: Call Hierarchy: display search scope in view title)
 *   Stephan Herrmann (stephan@cs.tu-berlin.de):
 *          - bug 75800: [call hierarchy] should allow searches for fields
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;


class SearchScopeHierarchyAction extends SearchScopeAction {
	//TODO: does such a scope make sense at all?

	private final SearchScopeActionGroup fGroup;

	public SearchScopeHierarchyAction(SearchScopeActionGroup group) {
		super(group, CallHierarchyMessages.SearchScopeActionGroup_hierarchy_text);
		this.fGroup = group;
		setToolTipText(CallHierarchyMessages.SearchScopeActionGroup_hierarchy_tooltip);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_SEARCH_SCOPE_ACTION);
	}

	public IJavaSearchScope getSearchScope() {
		try {
			IMember[] members = fGroup.getView().getInputElements();

			if (members != null && members.length == 1) {
				IType type= members[0] instanceof IType ? (IType) members[0] : members[0].getDeclaringType();
				return SearchEngine.createHierarchyScope(type);
			} else {
				return null;
			}
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.callhierarchy.SearchScopeActionGroup.SearchScopeAction#getSearchScopeType()
	 */
	public int getSearchScopeType() {
		return SearchScopeActionGroup.SEARCH_SCOPE_TYPE_HIERARCHY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.callhierarchy.SearchScopeAction#getFullDescription()
	 */
	public String getFullDescription() {
		IMember[] members= fGroup.getView().getInputElements();
		if (members != null && members.length == 1) {
			IType type= members[0] instanceof IType ? (IType) members[0] : members[0].getDeclaringType();
			return JavaSearchScopeFactory.getInstance().getHierarchyScopeDescription(type);
		} else {
			return JavaSearchScopeFactory.getInstance().getWorkspaceScopeDescription(true);
		}
	}

}
