/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;

import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IActionDelegate;

import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;

/** In plugin.xml:
   <extension
         point="org.eclipse.ui.popupMenus">
      <objectContribution
            objectClass="org.eclipse.core.resources.IProject"
            id="org.eclipse.jdt.ui.examples.AddJavaNatureAction">
         <action
               label="Add Java Nature (jdt.ui.tests)"
               tooltip="Add Java Nature"
               class="org.eclipse.jdt.ui.examples.AddJavaNatureAction"
               menubarPath="AddJavaNauture"
               enablesFor="1"
               id="addJavaNature">
         </action>
      </objectContribution>
   </extension>
 */

public class AddJavaNatureAction extends Action implements IActionDelegate {

	private IProject fProject;


	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		try {
			if (fProject != null) {
				BuildPathsBlock.addJavaNature(fProject, null);
			}
		} catch (Exception e) {
			JavaTestPlugin.log(e);
		}

	}
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fProject= null;
		if (selection instanceof IStructuredSelection) {
			Object object= ((IStructuredSelection) selection).getFirstElement();
			if (object instanceof IProject) {
				fProject= (IProject) object;

			}
		}
	}

}
