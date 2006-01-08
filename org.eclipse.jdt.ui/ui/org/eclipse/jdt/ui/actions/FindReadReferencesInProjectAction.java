/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchPage;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

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
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 */
	public FindReadReferencesInProjectAction(JavaEditor editor) {
		super(editor);
	}

	void init() {
		setText(SearchMessages.Search_FindReadReferencesInProjectAction_label); 
		setToolTipText(SearchMessages.Search_FindReadReferencesInProjectAction_tooltip); 
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_REF);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_READ_REFERENCES_IN_PROJECT_ACTION);
	}
	
	IJavaSearchScope getScope(IJavaElement element) {
		JavaSearchScopeFactory instance= JavaSearchScopeFactory.getInstance();
		JavaEditor editor= getEditor();
		if (editor != null) {
			return instance.createJavaProjectSearchScope(editor.getEditorInput(), JavaSearchPage.getSearchJRE());
		} else {
			return instance.createJavaProjectSearchScope(element.getJavaProject(), JavaSearchPage.getSearchJRE());
		}
	}

	String getScopeDescription(IJavaElement element) {
		JavaSearchScopeFactory instance= JavaSearchScopeFactory.getInstance();
		JavaEditor editor= getEditor();
		if (editor != null) {
			return instance.getProjectScopeDescription(editor.getEditorInput());
		} else {
			return instance.getProjectScopeDescription(element.getJavaProject());
		}
	}

}
