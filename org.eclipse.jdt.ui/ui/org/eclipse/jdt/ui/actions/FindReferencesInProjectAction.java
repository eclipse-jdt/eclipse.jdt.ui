/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.core.resources.IResource;

import org.eclipse.ui.IWorkbenchSite;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Finds references to the selected element in the projects of the 
 * selected elements.
 * The action is applicable to selections representing a Java element.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class FindReferencesInProjectAction extends FindReferencesAction {

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.actions.FindAction#canOperateOn(org.eclipse.jdt.core.IJavaElement)
	 */
	boolean canOperateOn(IJavaElement element) {
		// TODO Auto-generated method stub
		return super.canOperateOn(element);
	}

	/**
	 * Creates a new <code>FindReferencesInProjectAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindReferencesInProjectAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindDeclarationsInProjectsAction.label"), new Class[] {IField.class, IMethod.class, IType.class, ICompilationUnit.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationsInProjectsAction.tooltip")); //$NON-NLS-1$
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindReferencesInProjectAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindDeclarationsInProjectsAction.label"), new Class[] {IField.class, IMethod.class, IType.class, ICompilationUnit.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationsInProjectsAction.tooltip")); //$NON-NLS-1$
	}

	IJavaSearchScope getScope(IType element) throws JavaModelException {
		return JavaSearchScopeFactory.getInstance().createJavaSearchScope(new IResource[] { element.getJavaProject().getProject() });
	}

	String getScopeDescription(IType type) {
		return SearchMessages.getString("ProjectScope"); //$NON-NLS-1$
	}


}
