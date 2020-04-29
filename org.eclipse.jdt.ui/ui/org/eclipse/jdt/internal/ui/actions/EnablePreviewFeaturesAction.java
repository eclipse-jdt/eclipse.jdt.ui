/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IProject;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.CompliancePreferencePage;
import org.eclipse.jdt.internal.ui.text.correction.PreviewFeaturesSubProcessor;
import org.eclipse.jdt.internal.ui.util.ClasspathVMUtil;

public class EnablePreviewFeaturesAction implements IObjectActionDelegate {

	private ISelection fSelection;

	private IJavaProject fJavaProject;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
		fJavaProject= getJavaProject();
		action.setEnabled(!PreviewFeaturesSubProcessor.isPreviewFeatureEnabled(fJavaProject));
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// not used
	}

	@Override
	public void run(IAction action) {
		if (fJavaProject != null) {
			try {

				if (!JavaModelUtil.isLatestOrHigherJavaVersion(fJavaProject)) {
					String latestSupportedJavaVersion= JavaCore.latestSupportedJavaVersion();
					IVMInstall install= ClasspathVMUtil.findRequiredOrGreaterVMInstall(latestSupportedJavaVersion, true, true);
					if (install == null) {
						MessageDialog.openError(getDisplay().getActiveShell(), ActionMessages.EnablePreviewFeaturesAction_error_title,
								Messages.format(ActionMessages.EnablePreviewFeaturesAction_error_message_compliance, latestSupportedJavaVersion));
						return;
					} else {
						String compliance= ClasspathVMUtil.getVMInstallCompliance(install, false);
						if (compliance != null) {
							boolean val= MessageDialog.openQuestion(getDisplay().getActiveShell(), ActionMessages.EnablePreviewFeaturesAction_error_title,
									Messages.format(ActionMessages.EnablePreviewFeaturesAction_convert_message_compliance, compliance));
							if (!val) {
								return;
							} else {
								updateJRE(fJavaProject, compliance);
								updateComplianceSettings(fJavaProject, compliance);
							}
						} else {
							MessageDialog.openError(getDisplay().getActiveShell(), ActionMessages.EnablePreviewFeaturesAction_error_title,
									Messages.format(ActionMessages.EnablePreviewFeaturesAction_error_message_compliance, compliance));
							return;
						}
					}
				}

				Map<String, String> map= fJavaProject.getOptions(false);
				map.put(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
				fJavaProject.setOptions(map);

				Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				Map<String, Object> data= new HashMap<>();
				data.put(CompliancePreferencePage.DATA_SELECT_OPTION_KEY, JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES);
				data.put(CompliancePreferencePage.DATA_SELECT_OPTION_QUALIFIER, JavaCore.PLUGIN_ID);
				data.put(CompliancePreferencePage.USE_PROJECT_SPECIFIC_OPTIONS, Boolean.TRUE);
				PreferencesUtil.createPropertyDialogOn(shell, fJavaProject, CompliancePreferencePage.PROP_ID, null, data).open();

			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private IJavaProject getJavaProject() {
		IJavaProject javaProject= null;

		if (fSelection instanceof IStructuredSelection) {
			Object selectedElement= ((IStructuredSelection) fSelection).getFirstElement();

			if (selectedElement instanceof IProject) {
				javaProject= JavaCore.create((IProject) selectedElement);
			} else if (selectedElement instanceof IJavaProject) {
				javaProject= (IJavaProject) selectedElement;
			}
		}

		return javaProject;
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
