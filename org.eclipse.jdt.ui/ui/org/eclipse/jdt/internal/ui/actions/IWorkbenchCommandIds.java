/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;


/**
 * This interface collects commandIds (a.k.a. actionDefinitionIds) that should
 * be declared in a workbench API, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=54581 .
 * 
 * XXX: Remove this interface once Platform/UI manages to provide this API.
 * 
 * @see org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds
 * @sicen 3.4
 */
public interface IWorkbenchCommandIds {
	
	public static final String LINK_WITH_EDITOR= "org.eclipse.ui.navigate.linkWithEditor"; //$NON-NLS-1$
	

//	public static final String COLLAPSE_ALL= CollapseAllHandler.COMMAND_ID;
	
	public static final String SHOW_VIEW_MENU= "org.eclipse.ui.window.showViewMenu"; //$NON-NLS-1$
	
	public static final String SHOW_IN_QUICK_MENU= "org.eclipse.ui.navigate.showInQuickMenu"; //$NON-NLS-1$
	
	public static final String REFRESH= "org.eclipse.ui.file.refresh"; //$NON-NLS-1$


	public static final String BUILD_PROJECT= "org.eclipse.ui.project.buildProject"; //$NON-NLS-1$

	public static final String CLOSE_PROJECT= "org.eclipse.ui.project.closeProject"; //$NON-NLS-1$

	public static final String CLOSE_UNRELATED_PROJECTS= "org.eclipse.ui.project.closeUnrelatedProjects"; //$NON-NLS-1$

	public static final String OPEN_PROJECT= "org.eclipse.ui.project.openProject"; //$NON-NLS-1$

	
}
