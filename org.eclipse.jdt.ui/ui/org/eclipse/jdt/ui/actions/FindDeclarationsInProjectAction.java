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

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchScope;


import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchQuery;
import org.eclipse.jdt.internal.ui.search.JavaSearchOperation;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.jdt.ui.search.ElementQuerySpecification;

/**
 * Finds declarations of the selected element in the enclosing project 
 * of the selected element.
 * The action is applicable to selections representing a Java element.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class FindDeclarationsInProjectAction extends FindDeclarationsAction {

	/**
	 * Creates a new <code>FindDeclarationsInProjectAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindDeclarationsInProjectAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindDeclarationsInProjectAction.label"), new Class[] {IField.class, IMethod.class, IType.class, ICompilationUnit.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class, ILocalVariable.class}); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationsInProjectAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_DECLARATIONS_IN_PROJECT_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindDeclarationsInProjectAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindDeclarationsInProjectAction.label"), new Class[] {IField.class, IMethod.class, IType.class, ICompilationUnit.class, IPackageDeclaration.class, IImportDeclaration.class, IPackageFragment.class, ILocalVariable.class }); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindDeclarationsInProjectAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_DECLARATIONS_IN_PROJECT_ACTION);
	}

	IJavaSearchScope getScope(IJavaElement element) {
		return JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(element, true);
	}

	String getScopeDescription(IJavaElement element) {
		return SearchUtil.getProjectScopeDescription(element);
	}

	JavaSearchOperation makeOperation(IJavaElement element) {
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(element), getScopeDescription(element), getCollector());
	}

	protected JavaSearchQuery createJob(IJavaElement element) {
		return new JavaSearchQuery(new ElementQuerySpecification(element, getLimitTo(), getScope(element), getScopeDescription(element)));
	}

}
