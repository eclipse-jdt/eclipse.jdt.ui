/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class FindHierarchyDeclarationsAction extends FindDeclarationsAction {

	public FindHierarchyDeclarationsAction() {
		super(JavaPlugin.getResourceString("Search.FindHierarchyDeclarationsAction.label"), new Class[] {IField.class, IMethod.class, IType.class} );
		setToolTipText(JavaPlugin.getResourceString("Search.FindHierarchyDeclarationsAction.tooltip"));
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
}