/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkingSet;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

public class FindDeclarationsInWorkingSetAction extends FindDeclarationsAction {

	private IWorkingSet[] fWorkingSet;

	public FindDeclarationsInWorkingSetAction(IWorkbenchSite site) {
		super(site);
		init();
	}

	public FindDeclarationsInWorkingSetAction(JavaEditor editor) {
		super(editor);
		init();
	}

	public FindDeclarationsInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets) {
		this(site);
		fWorkingSet= workingSets;
	}

	public FindDeclarationsInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets) {
		this(editor);
		fWorkingSet= workingSets;
	}

	private void init() {
		setText(SearchMessages.getString("Search.FindDeclarationsInWorkingSetAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationsInWorkingSetAction.tooltip")); //$NON-NLS-1$
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IWorkingSet[] workingSets= fWorkingSet;
		if (fWorkingSet == null) {
			workingSets= JavaSearchScopeFactory.getInstance().queryWorkingSets();
			if (workingSets == null)
				return null;
		}
		updateLRUWorkingSets(workingSets);
		if (element.getElementType() == IJavaElement.METHOD) {
			IMethod method= (IMethod)element;
			int searchFor= IJavaSearchConstants.METHOD;
			if (method.isConstructor())
				searchFor= IJavaSearchConstants.CONSTRUCTOR;
			String pattern= PrettySignature.getUnqualifiedMethodSignature(method);
			return new JavaSearchOperation(JavaPlugin.getWorkspace(), pattern, true, searchFor, getLimitTo(), getScope(workingSets), getScopeDescription(workingSets), getCollector());
		}
		else
			return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(workingSets), getScopeDescription(workingSets), getCollector());
	};


	private IJavaSearchScope getScope(IWorkingSet[] workingSets) throws JavaModelException {
		return JavaSearchScopeFactory.getInstance().createJavaSearchScope(workingSets);
	}

	private String getScopeDescription(IWorkingSet[] workingSets) {
		return SearchMessages.getFormattedString("WorkingSetScope", new String[] {SearchUtil.toString(workingSets)}); //$NON-NLS-1$

	}
}
