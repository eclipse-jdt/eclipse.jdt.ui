/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class FindDeclarationsInHierarchyAction extends FindDeclarationsAction {

	public FindDeclarationsInHierarchyAction() {
		super(SearchMessages.getString("Search.FindHierarchyDeclarationsAction.label"), new Class[] {IField.class, IMethod.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyDeclarationsAction.tooltip")); //$NON-NLS-1$
	}

	protected JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		IType type= getType(element);
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(type), getScopeDescription(type), getCollector());
	};
	
	protected IJavaSearchScope getScope(IType type) throws JavaModelException {
		if (type != null)
			return SearchEngine.createHierarchyScope(type);
		else
			return super.getScope(type);
	}
	
	protected String getScopeDescription(IType type) {
		String typeName= ""; //$NON-NLS-1$
		if (type != null)
			typeName= type.getElementName();
		return SearchMessages.getFormattedString("HierarchyScope", new String[] {typeName}); //$NON-NLS-1$
	}
}