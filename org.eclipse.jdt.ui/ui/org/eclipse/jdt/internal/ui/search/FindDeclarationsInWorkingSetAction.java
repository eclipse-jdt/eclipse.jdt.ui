/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.search.ui.IWorkingSet;
import org.eclipse.search.ui.SearchUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.util.Assert;

public class FindDeclarationsInWorkingSetAction extends FindDeclarationsAction {

	private IWorkingSet fWorkingSet;

	public FindDeclarationsInWorkingSetAction(IWorkingSet workingSet) {
		this();
		fWorkingSet= workingSet;
	}

	public FindDeclarationsInWorkingSetAction() {
		setText(SearchMessages.getString("Search.FindDeclarationsInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationsInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet workingSet= fWorkingSet;
		if (fWorkingSet == null) {
			workingSet= queryWorkingSet();
			if (workingSet == null)
				return null;
		}
		updateLRUWorkingSet(workingSet);
		if (element.getElementType() == IJavaElement.METHOD) {
			IMethod method= (IMethod)element;
			int searchFor= IJavaSearchConstants.METHOD;
			if (method.isConstructor())
				searchFor= IJavaSearchConstants.CONSTRUCTOR;
			String pattern= PrettySignature.getUnqualifiedMethodSignature(method);
			return new JavaSearchOperation(JavaPlugin.getWorkspace(), pattern, searchFor, getLimitTo(), getScope(workingSet), getScopeDescription(workingSet), getCollector());
		}
		else
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
