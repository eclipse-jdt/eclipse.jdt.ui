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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchPage;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.search.SearchUtil;

/**
 * Finds field read accesses of the selected element in the enclosing project.
 * The action is applicable to selections representing a Java field.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class FindReadReferencesInProjectAction extends FindReadReferencesAction {

	/**
	 * Creates a new <code>FindReadReferencesInProjectAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindReadReferencesInProjectAction(IWorkbenchSite site) {
		super(site);
		init();
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindReadReferencesInProjectAction(JavaEditor editor) {
		super(editor);
		init();
	}

	private void init() {
		setText(SearchMessages.getString("Search.FindReadReferencesInProjectAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesInProjectAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_READ_REFERENCES_IN_PROJECT_ACTION);
	}
	
	IJavaSearchScope getScope(IJavaElement element) throws JavaModelException {
		return JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(element, JavaSearchPage.getSearchJRE());
	}

	String getScopeDescription(IJavaElement type) {
		return SearchUtil.getProjectScopeDescription(type);
	}


}
