/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class FindImplementorsInWorkingSetAction extends FindImplementorsAction {

	private IWorkingSet fWorkingSet;

	public FindImplementorsInWorkingSetAction(IWorkingSet workingSet) {
		this();
		fWorkingSet= workingSet;
	}

	public FindImplementorsInWorkingSetAction() {
		setText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet workingSet= fWorkingSet;
		if (fWorkingSet == null) {
			workingSet= JavaSearchScopeFactory.getInstance().queryWorkingSet();
			if (workingSet == null)
				return null;
		}
		updateLRUWorkingSet(workingSet);
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(workingSet), getScopeDescription(workingSet), getCollector());
	};


	private IJavaSearchScope getScope(IWorkingSet workingSet) throws JavaModelException {
		return JavaSearchScopeFactory.getInstance().createJavaSearchScope(workingSet);
	}

	private String getScopeDescription(IWorkingSet workingSet) {
		return SearchMessages.getFormattedString("WorkingSetScope", new String[] {workingSet.getName()}); //$NON-NLS-1$

	}
}

