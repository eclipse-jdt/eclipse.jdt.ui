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

	private IWorkingSet[] fWorkingSets;

	public FindImplementorsInWorkingSetAction(IWorkingSet[] workingSet) {
		this();
		fWorkingSets= workingSet;
	}

	public FindImplementorsInWorkingSetAction() {
		setText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindImplementorsInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet[] workingSets= fWorkingSets;
		if (fWorkingSets == null) {
			workingSets= JavaSearchScopeFactory.getInstance().queryWorkingSets();
			if (workingSets == null)
				return null;
		}
		updateLRUWorkingSets(workingSets);
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(workingSets), getScopeDescription(workingSets), getCollector());
	};


	private IJavaSearchScope getScope(IWorkingSet[] workingSets) throws JavaModelException {
		return JavaSearchScopeFactory.getInstance().createJavaSearchScope(workingSets);
	}

	private String getScopeDescription(IWorkingSet[] workingSets) {
		return SearchMessages.getFormattedString("WorkingSetScope", new String[] {SearchUtil.toString(workingSets)}); //$NON-NLS-1$

	}
}

