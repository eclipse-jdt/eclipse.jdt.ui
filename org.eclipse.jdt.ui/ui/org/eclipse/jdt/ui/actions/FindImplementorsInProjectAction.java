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

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchOperation;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.search.SearchUtil;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Finds implementors of the selected element in the enclosing project.
 * The action is applicable to selections representing a Java interface.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class FindImplementorsInProjectAction extends FindImplementorsAction {

	/**
	 * Creates a new <code>FindImplementorsInProjectAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindImplementorsInProjectAction(IWorkbenchSite site) {
		super(site);
		init();
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindImplementorsInProjectAction(JavaEditor editor) {
		super(editor);
		init();
	}

	private void init() {
		setText(SearchMessages.getString("Search.FindImplementorsInProjectAction.label")); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindImplementorsInProjectAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_IMPLEMENTORS_IN_PROJECT_ACTION);
	}
	
	IJavaSearchScope getScope(IJavaElement element) throws JavaModelException {
		return JavaSearchScopeFactory.getInstance().createJavaProjectSearchScope(element);
	}

	String getScopeDescription(IJavaElement element) {
		return SearchUtil.getProjectScopeDescription(element);
	}

	JavaSearchOperation makeOperation(IJavaElement element) throws JavaModelException {
		return new JavaSearchOperation(JavaPlugin.getWorkspace(), element, getLimitTo(), getScope(element), getScopeDescription(element), getCollector());
	}
}
