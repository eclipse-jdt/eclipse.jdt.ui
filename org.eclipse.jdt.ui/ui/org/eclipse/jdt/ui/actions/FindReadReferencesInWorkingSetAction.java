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
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.search.IJavaSearchConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.search.SearchMessages;

/**
 * Finds field read accesses of the selected element in working sets.
 * The action is applicable to selections representing a Java field.
 * 
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * 
 * @since 2.0
 */
public class FindReadReferencesInWorkingSetAction extends FindReferencesInWorkingSetAction {

	/**
	 * Creates a new <code>FindReadReferencesInWorkingSetAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>. The user will be 
	 * prompted to select the working sets.
	 * 
	 * @param site the site providing context information for this action
	 */
	public FindReadReferencesInWorkingSetAction(IWorkbenchSite site) {
		super(site, SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.label"), new Class[] {IField.class, ILocalVariable.class } ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_READ_REFERENCES_IN_WORKING_SET_ACTION);
	}

	/**
	 * Creates a new <code>FindReadReferencesInWorkingSetAction</code>. The action 
	 * requires that the selection provided by the site's selection provider is of type 
	 * <code>org.eclipse.jface.viewers.IStructuredSelection</code>.
	 * 
	 * @param site			the site providing context information for this action
	 * @param workingSets	the working sets to be used in the search
	 */
	public FindReadReferencesInWorkingSetAction(IWorkbenchSite site, IWorkingSet[] workingSets) {
		super(site, workingSets, new Class[] {IField.class, ILocalVariable.class });
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_READ_REFERENCES_IN_WORKING_SET_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindReadReferencesInWorkingSetAction(JavaEditor editor) {
		super(editor, SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.label"), new Class[] {IField.class, ILocalVariable.class } ); //$NON-NLS-1$
		setToolTipText(SearchMessages.getString("Search.FindReadReferencesInWorkingSetAction.tooltip")); //$NON-NLS-1$
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_READ_REFERENCES_IN_WORKING_SET_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public FindReadReferencesInWorkingSetAction(JavaEditor editor, IWorkingSet[] workingSets) {
		super(editor, workingSets, new Class[] {IField.class, ILocalVariable.class});
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.FIND_READ_REFERENCES_IN_WORKING_SET_ACTION);
	}

	int getLimitTo() {
		return IJavaSearchConstants.READ_ACCESSES;
	}

	String getOperationUnavailableMessage() {
		return SearchMessages.getString("JavaElementAction.operationUnavailable.field"); //$NON-NLS-1$
	}
}
