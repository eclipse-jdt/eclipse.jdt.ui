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
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class FindHierarchyReferencesAction extends FindReferencesAction {

	public FindHierarchyReferencesAction() {
		super(SearchMessages.getString("Search.FindHierarchyReferencesAction.label"), new Class[] {IField.class, IMethod.class, IType.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyReferencesAction.tooltip")); //$NON-NLS-1$
	}

	protected IJavaSearchScope getScope(IJavaElement element) throws JavaModelException {
		IType type= getType(element);
		if (type == null)
			return super.getScope(element);
		return SearchEngine.createHierarchyScope(getType(element));
	}
	
	protected boolean shouldUserBePrompted() {
		return true;
	}

	protected String getScopeDescription(IJavaElement element) {
		IType type= getType(element);
		String typeName= ""; //$NON-NLS-1$
		if (type != null)
			typeName= type.getElementName();
		return SearchMessages.getFormattedString("HierarchyScope", new String[] {typeName}); //$NON-NLS-1$
	}

	private IType getType(IJavaElement element) {
		IType type= null;
		if (element.getElementType() == IJavaElement.TYPE)
			type= (IType)element;
		else if (element.getElementType() == IJavaElement.METHOD) {
			IMethod method= (IMethod)element;
			type= method.getDeclaringType();
		}
		if (type != null) {
			ICompilationUnit cu= type.getCompilationUnit();
			if (cu != null && cu.isWorkingCopy())
				return (IType)cu.getOriginal(type);
		}
		return null;
	}
}