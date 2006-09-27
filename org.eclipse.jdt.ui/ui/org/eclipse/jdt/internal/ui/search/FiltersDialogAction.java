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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;

import org.eclipse.search.ui.text.MatchFilter;
import org.eclipse.search.ui.text.MatchFilterSelectionDialog;


public class FiltersDialogAction extends Action {
	private JavaSearchResultPage fPage;
	
	public FiltersDialogAction(JavaSearchResultPage page) {
		super(SearchMessages.FiltersDialogAction_label); 
		fPage= page;
	}

	public void run() {
		Shell shell= fPage.getSite().getShell();
		MatchFilter[] allFilters= JavaMatchFilter.allFilters();
		MatchFilter[] checkedFilters= fPage.getMatchFilters();
		int limit= fPage.getElementLimit();
		
		MatchFilterSelectionDialog dialog = new MatchFilterSelectionDialog(shell, allFilters, checkedFilters, true, limit);
		dialog.setTitle(SearchMessages.FiltersDialog_title);
		
		if (dialog.open() == Window.OK) {
			fPage.setFilters(dialog.getMatchFilters());
			fPage.setElementLimit(dialog.getLimit());
		}
	}

}
