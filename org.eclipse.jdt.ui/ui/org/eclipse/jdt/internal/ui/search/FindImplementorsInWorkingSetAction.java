/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.search.ui.IWorkingSet;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class FindImplementorsInWorkingSetAction extends FindImplementorsAction {

	public FindImplementorsInWorkingSetAction() {
		setText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet workingSet= queryWorkingSet();
		if (workingSet == null)
			return null;
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(workingSet), getScopeDescription(workingSet), getCollector());
	};


	private IJavaSearchScope getScope(IWorkingSet workingSet) throws JavaModelException {
		return SearchEngine.createJavaSearchScope(workingSet.getResources());
	}

	private String getScopeDescription(IWorkingSet workingSet) {
		return SearchMessages.getFormattedString("WorkingSetScope", new String[] {workingSet.getName()}); //$NON-NLS-1$

	}

	private IWorkingSet queryWorkingSet() throws JavaModelException {
		SelectionDialog dialog= SearchUI.createWorkingSetDialog(JavaPlugin.getActiveWorkbenchShell());
		if (dialog.open() == dialog.OK)
			return (IWorkingSet)dialog.getResult()[0];
		else
			return null;
	}
}

