/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

public class FindHierarchyDeclarationsAction extends FindDeclarationsAction {

	public FindHierarchyDeclarationsAction() {
		super(SearchMessages.getString("Search.FindHierarchyDeclarationsAction.label"), new Class[] {IField.class, IMethod.class, IType.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyDeclarationsAction.tooltip")); //$NON-NLS-1$
	}
	
	protected IJavaSearchScope getScope(IType type) throws JavaModelException {
		ICompilationUnit cu= type.getCompilationUnit();
			if (cu != null && cu.isWorkingCopy()) {
				type= (IType)cu.getOriginal(type);
			}
		return SearchEngine.createHierarchyScope(type);
	}
	
	protected boolean shouldUserBePrompted() {
		return true;
	}

	protected String getScopeDescription(IType type) {
		return SearchMessages.getFormattedString("HierarchyScope", new String[] {type.getElementName()}); //$NON-NLS-1$
	}
}