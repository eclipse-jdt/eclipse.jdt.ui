/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

public class FindReferencesInHierarchyAction extends FindReferencesAction {

	public FindReferencesInHierarchyAction() {
		super(SearchMessages.getString("Search.FindHierarchyReferencesAction.label"), new Class[] {IType.class, IMethod.class, IField.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyReferencesAction.tooltip")); //$NON-NLS-1$
	}

	FindReferencesInHierarchyAction(String label, Class[] validTypes) {
		super(label, validTypes);
	}

	protected IJavaSearchScope getScope(IType type) throws JavaModelException {
		if (type == null)
			return super.getScope(type);
		return SearchEngine.createHierarchyScope(type);
	}
	
	protected String getScopeDescription(IType type) {
		String typeName= ""; //$NON-NLS-1$
		if (type != null)
			typeName= type.getElementName();
		return SearchMessages.getFormattedString("HierarchyScope", new String[] {typeName}); //$NON-NLS-1$
	}
}