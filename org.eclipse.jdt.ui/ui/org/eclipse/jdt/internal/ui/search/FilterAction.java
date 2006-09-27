/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;


public class FilterAction extends Action {
	private JavaMatchFilter fFilter;
	private JavaSearchResultPage fPage;
	
	public FilterAction(JavaSearchResultPage page, JavaMatchFilter filter) {
		super(filter.getActionLabel(), IAction.AS_CHECK_BOX);
		fPage= page;
		fFilter= filter;
	}

	public void run() {
		if (fPage.hasMatchFilter(getFilter())) {
			fPage.removeMatchFilter(fFilter);
		} else {
			fPage.addMatchFilter(fFilter);
		}
	}

	public JavaMatchFilter getFilter() {
		return fFilter;
	}

	public void updateCheckState() {
		setChecked(fPage.hasMatchFilter(getFilter()));
	}
}
