/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.actions;

import org.eclipse.jface.util.Assert;

import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;


import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;

/**
 * Wraps a <code>JavaElementSearchActions</code> to find its results
 * in the specified working set.
 * <p>
 * The action is applicable to selections and Search view entries
 * representing a Java element.
 * 
 * <p>
 * Note: This class is for internal use only. Clients should not use this class.
 * </p>
 * 
 * @since 2.0
 */
public class WorkingSetFindAction extends FindAction {

	private FindAction fAction;

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public WorkingSetFindAction(IWorkbenchSite site, FindAction action, String workingSetName) {
		super(site, workingSetName, null);
		Assert.isNotNull(action);
		fAction= action;
		setImageDescriptor(action.getImageDescriptor());
		setToolTipText(action.getToolTipText());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.WORKING_SET_FIND_ACTION);
	}

	/**
	 * Note: This constructor is for internal use only. Clients should not call this constructor.
	 */
	public WorkingSetFindAction(JavaEditor editor, FindAction action, String workingSetName) {
		super(editor, workingSetName, null);
		Assert.isNotNull(action);
		fAction= action;
		setImageDescriptor(action.getImageDescriptor());
		setToolTipText(action.getToolTipText());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.WORKING_SET_FIND_ACTION);
	}

	public void run(IJavaElement element) {
		fAction.run(element);
	}

	boolean canOperateOn(IJavaElement element) {
		return fAction.canOperateOn(element);
	}

	int getLimitTo() {
		return -1;
	}

	String getOperationUnavailableMessage() {
		return fAction.getOperationUnavailableMessage();
	}

}
