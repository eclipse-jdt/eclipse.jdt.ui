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

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class FindReferencesInWorkingSetAction extends FindReferencesAction {

	private IWorkingSet fWorkingSet;
	
	public FindReferencesInWorkingSetAction() {
		setText(SearchMessages.getString("Search.FindReferencesInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	FindReferencesInWorkingSetAction(String label, Class[] validTypes) {
		super(label, validTypes);
	}

	FindReferencesInWorkingSetAction(IWorkingSet workingSet, Class[] validTypes) {
		super("", validTypes);  //$NON-NLS-1$
		fWorkingSet= workingSet;
	}

	public FindReferencesInWorkingSetAction(IWorkingSet workingSet) {
		this();
		fWorkingSet= workingSet;
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet workingSet= fWorkingSet;
		if (fWorkingSet == null) {
			workingSet= queryWorkingSet();
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

	private IWorkingSet queryWorkingSet() throws JavaModelException {
		SelectionDialog dialog= SearchUI.createWorkingSetDialog(JavaPlugin.getActiveWorkbenchShell());
		if (dialog.open() == dialog.OK)
			return (IWorkingSet)dialog.getResult()[0];
		else
			return null;
	}
}
