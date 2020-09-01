/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWizard;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.ClasspathVMUtil;
import org.eclipse.jdt.internal.ui.wizards.NewModuleInfoWizard;

public class CreateModuleInfoAction implements IObjectActionDelegate {

	private static class ModuleInfoCreationDialog extends WizardDialog {

		public ModuleInfoCreationDialog(Shell parentShell, IWizard newWizard) {
			super(parentShell, newWizard);
		}

		@Override
		protected final void createButtonsForButtonBar(final Composite parent) {
			super.createButtonsForButtonBar(parent);
			Button cancel= this.getButton(CANCEL);
			Button finish= this.getButton(IDialogConstants.FINISH_ID);
			if (cancel != null) {
				cancel.setText(ActionMessages.CreateModuleInfoAction_dialog_cancel_button_label);
				setButtonLayoutData(cancel);
			}
			if (finish != null) {
				finish.setText(ActionMessages.CreateModuleInfoAction_dialog_finish_button_label);
				setButtonLayoutData(finish);
			}
		}

		@Override
		protected void setButtonLayoutData(Button button) {
			super.setButtonLayoutData(button);
			Object data= button.getLayoutData();
			if (data instanceof GridData) {
				GridData gridData= (GridData) data;
				gridData.widthHint+= button.getText().length();
				button.setLayoutData(gridData);
			}
		}
	}

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
					IVMInstall install= ClasspathVMUtil.findRequiredOrGreaterVMInstall(JavaCore.VERSION_9, true, true);
					if (install == null) {
						MessageDialog.openError(getDisplay().getActiveShell(), ActionMessages.CreateModuleInfoAction_error_title, ActionMessages.CreateModuleInfoAction_error_message_compliance);
						return;
					} else {
						String compliance= ClasspathVMUtil.getVMInstallCompliance(install, false);
						if (compliance != null) {
							boolean val= MessageDialog.openQuestion(getDisplay().getActiveShell(), ActionMessages.CreateModuleInfoAction_convert_title,
									Messages.format(ActionMessages.CreateModuleInfoAction_convert_message_compliance, compliance));
							if (!val) {
								return;
							} else {
								updateJRE(javaProject, compliance);
								updateComplianceSettings(javaProject, compliance);
							}
						}
						else {
							MessageDialog.openError(getDisplay().getActiveShell(), ActionMessages.CreateModuleInfoAction_error_title, ActionMessages.CreateModuleInfoAction_error_message_compliance);
							return;
						}
					}
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
				WizardDialog dialog= new ModuleInfoCreationDialog(getDisplay().getActiveShell(), moduleInfoWizard);
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

	private boolean updateJRE(IJavaProject project, String compliance) throws CoreException, JavaModelException {
		IExecutionEnvironment bestEE= ClasspathVMUtil.findBestMatchingEE(compliance);
		if (bestEE != null) {
			IPath newPath= JavaRuntime.newJREContainerPath(bestEE);
			boolean classpathUpdated= ClasspathVMUtil.updateClasspath(newPath, project, new NullProgressMonitor());
			return !classpathUpdated;
		}
		return true;
	}

	private void updateComplianceSettings(IJavaProject project, String compliance) {
		HashMap<String, String> defaultOptions= new HashMap<>();
		JavaCore.setComplianceOptions(compliance, defaultOptions);
		Iterator<Map.Entry<String, String>> it= defaultOptions.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> pair= it.next();
			project.setOption(pair.getKey(), pair.getValue());
		}
	}
}
