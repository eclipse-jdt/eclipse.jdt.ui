/*
 * Copyright (c) 2000, 2002 IBM Corp. and others..
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.ui.actions;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Finds declarations of the selected element in its hierarchy.
 * The action is applicable for selections representing a Java element.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindDeclarationsInHierarchyAction extends FindDeclarationsAction {

	/**
	 * Creates a new <code>FindDeclarationsInHierarchyAction</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindDeclarationsInHierarchyAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindHierarchyDeclarationsAction.label"), new Class[] {IField.class, IMethod.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyDeclarationsAction.tooltip")); //$NON-NLS-1$
	}

	/**
	 * Creates a new <code>FindDeclarationsInHierarchyAction</code>.
	 * <p>
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * </p>
	 */
	public FindDeclarationsInHierarchyAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindHierarchyDeclarationsAction.label"), new Class[] {IField.class, IMethod.class} ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindHierarchyDeclarationsAction.tooltip")); //$NON-NLS-1$
	}
	
	IJavaSearchScope getScope(IType type) throws JavaModelException {
		if (type != null)
			return SearchEngine.createHierarchyScope(type);
		else
			return super.getScope(type);
	}
	
	String getScopeDescription(IType type) {
		String typeName= ""; //$NON-NLS-1$
		if (type != null)
			typeName= type.getElementName();
		return SearchMessages.getFormattedString("HierarchyScope", new String[] {typeName}); //$NON-NLS-1$
	}
}