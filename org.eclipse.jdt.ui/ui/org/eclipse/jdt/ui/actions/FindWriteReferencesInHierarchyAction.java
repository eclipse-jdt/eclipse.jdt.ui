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

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.search.SearchMessages;
import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Finds field write accesses of the selected element in its hierarchy.
 * The action is applicable to selections representing a Java field.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindWriteReferencesInHierarchyAction extends FindReferencesInHierarchyAction {

	/**
	 * Creates a new <code>FindWriteReferencesInHierarchyAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindWriteReferencesInHierarchyAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.label"), new Class[] {IField.class, ILocalVariable.class } ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_WRITE_REFERENCES_IN_HIERARCHY_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindWriteReferencesInHierarchyAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.label"), new Class[] {IField.class, ILocalVariable.class } ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindWriteReferencesInHierarchyAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_WRITE_REFERENCES_IN_HIERARCHY_ACTION);
	}

	int getLimitTo() {
		return IJavaSearchConstants.WRITE_ACCESSES;
	}

	String getOperationUnavailableMessage() {
		return SearchMessages.getString("JavaElementAction.operationUnavailable.field"); //$NON-NLS-1$
	}
}
