/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWizard;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.NewModuleInfoWizard;

public class CreateModuleInfoAction implements IObjectActionDelegate {

	private static final String MODULE_INFO_JAVA_FILENAME= JavaModelUtil.MODULE_INFO_JAVA;

	private ISelection fSelection;
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// not used
	}

	@Override
	public void run(IAction action) {
		IJavaProject javaProject= null;

		if (fSelection instanceof IStructuredSelection) {
			Object selectedElement= ((IStructuredSelection) fSelection).getFirstElement();

			if (selectedElement instanceof IProject) {
				javaProject= JavaCore.create((IProject) selectedElement);
			} else if (selectedElement instanceof IJavaProject) {
				javaProject= (IJavaProject) selectedElement;
			} else {
				return;
			}

			try {
				if (!JavaModelUtil.is9OrHigher(javaProject)) {
					MessageDialog.openError(getDisplay().getActiveShell(), ActionMessages.CreateModuleInfoAction_error_title, ActionMessages.CreateModuleInfoAction_error_message_compliance);
					return;
				}

				IPackageFragmentRoot[] packageFragmentRoots= javaProject.getPackageFragmentRoots();
				List<IPackageFragmentRoot> packageFragmentRootsAsList= new ArrayList<>(Arrays.asList(packageFragmentRoots));
				for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
					IResource res= packageFragmentRoot.getCorrespondingResource();
					if (res == null || res.getType() != IResource.FOLDER || packageFragmentRoot.getKind() != IPackageFragmentRoot.K_SOURCE) {
						packageFragmentRootsAsList.remove(packageFragmentRoot);
					}
				}
				packageFragmentRoots= packageFragmentRootsAsList.toArray(new IPackageFragmentRoot[packageFragmentRootsAsList.size()]);

				if (packageFragmentRoots.length == 0) {
					MessageDialog.openError(getDisplay().getActiveShell(), ActionMessages.CreateModuleInfoAction_error_title, ActionMessages.CreateModuleInfoAction_error_message_no_source_folder);
					return;
				}

				IPackageFragmentRoot targetPkgFragmentRoot= null;

				for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
					if (packageFragmentRoot.getPackageFragment("").getCompilationUnit(MODULE_INFO_JAVA_FILENAME).exists()) { //$NON-NLS-1$
						String message= Messages.format(ActionMessages.CreateModuleInfoAction_question_message_overwrite_module_info, packageFragmentRoot.getElementName());
						boolean overwrite= MessageDialog.openQuestion(getDisplay().getActiveShell(), ActionMessages.CreateModuleInfoAction_error_title, message);
						if (!overwrite) {
							return;
						}
						targetPkgFragmentRoot= packageFragmentRoot;
						break;
					}
				}

				IWorkbenchWizard moduleInfoWizard= new NewModuleInfoWizard(javaProject, packageFragmentRoots, targetPkgFragmentRoot);
				WizardDialog dialog= new WizardDialog(getDisplay().getActiveShell(), moduleInfoWizard);
				dialog.setHelpAvailable(false);
				dialog.create();				
				dialog.open();
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private Display getDisplay() {
		Display display= Display.getCurrent();
		if (display == null)
			display= Display.getDefault();
		return display;
	}
}
