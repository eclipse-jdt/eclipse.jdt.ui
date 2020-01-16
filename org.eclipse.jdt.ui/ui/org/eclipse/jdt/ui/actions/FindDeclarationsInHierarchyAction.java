/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - add support for restricting to sub-types
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeParameter;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.search.ElementQuerySpecification;
import org.eclipse.jdt.ui.search.QuerySpecification;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Finds declarations of the selected element in its hierarchy.
 * The action is applicable to selections representing a Java element.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 * @since 2.0
 *
 * @noextend This class is not intended to be subclassed by clients.
 */
public class FindDeclarationsInHierarchyAction extends FindDeclarationsAction {

	private boolean onlySubtypes;

	/**
	 * Creates a new <code>FindDeclarationsInHierarchyAction</code>. The action
	 * requires that the selection provided by the site's selection provider is of type
	 * <code>IStructuredSelection</code>.
	 *
	 * @param site the site providing context information for this action
	 */
	public FindDeclarationsInHierarchyAction(IWorkbenchSite site) {
		super(site);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 */
	public FindDeclarationsInHierarchyAction(JavaEditor editor) {
		super(editor);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 * @param editor the Java editor
	 * @param onlySubtypes true if only sub-types should be considered in search
	 *
	 * @noreference This constructor is not intended to be referenced by clients.
	 * @since 3.18
	 */
	public FindDeclarationsInHierarchyAction(JavaEditor editor, boolean onlySubtypes) {
		super(editor);
		this.onlySubtypes= onlySubtypes;
	}

	@Override
	Class<?>[] getValidTypes() {
		return new Class[] { IField.class, IMethod.class, ILocalVariable.class, ITypeParameter.class };
	}

	@Override
	void init() {
		setText(SearchMessages.Search_FindHierarchyDeclarationsAction_label);
		setToolTipText(SearchMessages.Search_FindHierarchyDeclarationsAction_tooltip);
		setImageDescriptor(JavaPluginImages.DESC_OBJS_SEARCH_DECL);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.FIND_DECLARATIONS_IN_HIERARCHY_ACTION);
	}

	@Override
	QuerySpecification createQuery(IJavaElement element) throws JavaModelException, InterruptedException {
		JavaSearchScopeFactory factory= JavaSearchScopeFactory.getInstance();

		IType type= getType(element);
		if (type == null) {
			return super.createQuery(element);
		}
		IJavaSearchScope scope= onlySubtypes
				? SearchEngine.createStrictHierarchyScope(null, type, true, false, null)
				: SearchEngine.createHierarchyScope(type);
		String description= factory.getHierarchyScopeDescription(type);
		return new ElementQuerySpecification(element, getLimitTo(), scope, description);
	}

	@Override
	boolean canOperateOn(IStructuredSelection sel) {
		if (sel == null || sel.isEmpty() || sel.size() > 1) {
			return false;
		}
		return super.canOperateOn(sel);
	}
}
