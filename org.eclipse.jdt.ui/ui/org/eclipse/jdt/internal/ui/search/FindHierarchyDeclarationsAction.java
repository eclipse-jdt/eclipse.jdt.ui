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
	
	protected IJavaSearchScope getScope(IJavaElement element) throws JavaModelException {
		if (element instanceof IType) {
			IType type= (IType)element;
			ICompilationUnit cu= type.getCompilationUnit();
				if (cu != null && cu.isWorkingCopy()) {
					type= (IType)cu.getOriginal(type);
				}
			return SearchEngine.createHierarchyScope(type);
		} else
			return super.getScope(element);
	}
	
	protected boolean shouldUserBePrompted() {
		return true;
	}

	protected String getScopeDescription(IJavaElement element) {
		if (element instanceof IJavaElement) 
			return SearchMessages.getFormattedString("HierarchyScope", new String[] {((IType)element).getElementName()}); //$NON-NLS-1$
		else
			return super.getScopeDescription(element);
	}
}