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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.ISelection;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowPulldownDelegate2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.internal.IWorkbenchConstants;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class NewTypeDropDownAction extends Action implements IMenuCreator, IWorkbenchWindowPulldownDelegate2 {

	private final static String TAG_WIZARD = "wizard";//$NON-NLS-1$
	private final static String ATT_JAVATYPE = "javatype";//$NON-NLS-1$

	private Menu fMenu;
	
	public NewTypeDropDownAction() {
		fMenu= null;
		setMenuCreator(this);
		WorkbenchHelp.setHelp(this, IJavaHelpContextIds.OPEN_CLASS_WIZARD_ACTION);
	}

	public void dispose() {
		if (fMenu != null) {
			fMenu.dispose();
			fMenu= null;
		}
	}

	public Menu getMenu(Menu parent) {
		return null;
	}

	public Menu getMenu(Control parent) {
		if (fMenu == null) {
			fMenu= new Menu(parent);
			
			Action[] actions= getActionFromDescriptors();
			for (int i= 0; i < actions.length; i++) {
				ActionContributionItem item= new ActionContributionItem(actions[i]);
				item.fill(fMenu, -1);				
			}			
		
		}
		return fMenu;
	}
	
	public void run() {
		(new OpenClassWizardAction()).run();
	}
	
	public static Action[] getActionFromDescriptors() {
		ArrayList containers= new ArrayList();
		
		IExtensionPoint extensionPoint = Platform.getPluginRegistry().getExtensionPoint(PlatformUI.PLUGIN_ID, IWorkbenchConstants.PL_NEW);
		if (extensionPoint != null) {
			IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
			for (int i = 0; i < elements.length; i++) {
				IConfigurationElement element= elements[i];
				if (element.getName().equals(TAG_WIZARD)) {
					if ("true".equals(element.getAttribute(ATT_JAVATYPE))) { //$NON-NLS-1$
						containers.add(new OpenTypeWizardAction(element));
					}
				}
			}
		}
		return (Action[]) containers.toArray(new Action[containers.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		run();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
