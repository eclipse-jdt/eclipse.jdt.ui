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
package org.eclipse.jdt.internal.ui.typehierarchy;

import org.eclipse.swt.custom.BusyIndicator;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Action enable / disable member filtering
 */
public class EnableMemberFilterAction extends Action {

	private TypeHierarchyViewPart fView;	
	
	public EnableMemberFilterAction(TypeHierarchyViewPart v, boolean initValue) {
		super(TypeHierarchyMessages.getString("EnableMemberFilterAction.label")); //$NON-NLS-1$
		setDescription(TypeHierarchyMessages.getString("EnableMemberFilterAction.description")); //$NON-NLS-1$
		setToolTipText(TypeHierarchyMessages.getString("EnableMemberFilterAction.tooltip")); //$NON-NLS-1$
		
		JavaPluginImages.setLocalImageDescriptors(this, "impl_co.gif"); //$NON-NLS-1$

		fView= v;
		setChecked(initValue);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.ENABLE_METHODFILTER_ACTION);
		
	}

	/*
	 * @see Action#actionPerformed
	 */		
	public void run() {
		BusyIndicator.showWhile(fView.getSite().getShell().getDisplay(), new Runnable() {
			public void run() {
				fView.enableMemberFilter(isChecked());
			}
		});
	}
}
