/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation 
 *          (report 36180: Callers/Callees view)
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.callhierarchy;

import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jface.action.Action;


abstract class SearchScopeAction extends Action {
	private final SearchScopeActionGroup fGroup;
	
	public SearchScopeAction(SearchScopeActionGroup group, String text) {
		super(text, AS_RADIO_BUTTON);
		this.fGroup = group;
	}
	
	public abstract IJavaSearchScope getSearchScope();
	
	public abstract int getSearchScopeType();
	
	public void run() {
		this.fGroup.setSelected(this, true);
	}
}
