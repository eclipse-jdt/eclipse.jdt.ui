/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.jdt.testplugin.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.JavaTestPlugin;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.binary.StubCreationOperation;

/**
 * See testresources/rtstubs-README.txt
 */
public final class CreateStubsAction implements IObjectActionDelegate {

	private static final String[] DEFAULT_PACKAGES= new String[] {
			"java.beans",
			"java.io",
			"java.lang",
			"java.lang.annotation",
			"java.lang.ref",
			"java.lang.reflect",
			"java.math",
			"java.net",
			"java.nio",
			"java.nio.channels",
			"java.nio.channels.spi",
			"java.nio.charset",
			"java.nio.charset.spi",
			"java.security",
			"java.security.cert",
			"java.security.interfaces",
			"java.security.spec",
			"java.sql",
			"java.text",
			"java.util",
			"java.util.jar",
			"java.util.regex",
			"java.util.zip",
			//1.7:
			"java.lang.invoke",
			"java.nio.file",
			"java.nio.file.attribute",
			"java.util.concurrent",
			//1.8:
			"java.util.function",
			"java.util.stream",
			"javax.annotation",
			"javax.annotation.processing",
			"javax.lang.model",
			"javax.lang.model.element",
			"javax.lang.model.type",
			"javax.lang.model.util",
			"javax.tools",
			//9:
			"java.lang.module",
			"java.nio.file.spi",
			//13:
			"java.lang.constant"
	};

	private static final String SETTINGS_ID_STUBS_PROJECT= "stubsProject";

	private static final String CREATE_STUBS_DIALOG_TITLE= "Create Stubs"; //$NON-NLS-1$

	private IWorkbenchPart fTargetPart;

	public CreateStubsAction() {
		super();
	}

	void createJavaProject(String projectName) throws CoreException {
		IJavaProject javaProject= JavaProjectHelper.createJavaProject(projectName, "bin");
		JavaProjectHelper.addSourceContainer(javaProject, "src");
	}

	private void fail(String msg) {
		Shell shell= fTargetPart.getSite().getShell();
		MessageDialog.openInformation(shell, CREATE_STUBS_DIALOG_TITLE, msg);
	}

	@Override
	public void run(IAction action) {
		ISelection selection= fTargetPart.getSite().getSelectionProvider().getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			fail("Select packages to create stubs."); //$NON-NLS-1$
			return;
		}
		final IStructuredSelection structuredSelection= (IStructuredSelection) selection;

		Shell shell= fTargetPart.getSite().getShell();
		String initialValue= JavaTestPlugin.getDefault().getDialogSettings().get(SETTINGS_ID_STUBS_PROJECT);
		if (initialValue == null)
			initialValue = "stubs"; //$NON-NLS-1$
		final InputDialog inputDialog= new InputDialog(shell, CREATE_STUBS_DIALOG_TITLE, "Target project name:", initialValue, newText -> {
			IStatus status = ResourcesPlugin.getWorkspace().validateName(newText, IResource.PROJECT);
			return status.isOK() ? null : (String) status.getMessage();
		});
		if (inputDialog.open() != Window.OK)
			return;
		try {

			final String name= inputDialog.getValue();
			JavaTestPlugin.getDefault().getDialogSettings().put(SETTINGS_ID_STUBS_PROJECT, name);
			long start= System.currentTimeMillis();

			ArrayList<String> defaultPackages= new ArrayList<>(Arrays.asList(DEFAULT_PACKAGES));

			ProgressMonitorDialog progressMonitorDialog= new ProgressMonitorDialog(shell);
			progressMonitorDialog.run(true, true, monitor -> {
				try {
					createJavaProject(name);
					IFolder target = ResourcesPlugin.getWorkspace().getRoot().getProject(inputDialog.getValue()).getFolder("src"); //$NON-NLS-1$
					List<IPackageFragment> packageFragments= new ArrayList<>();

					boolean checkCompletionOfDefaultPackages= false;
					for (Object sel : structuredSelection.toList()) {
						if (sel instanceof IPackageFragment) {
							packageFragments.add((IPackageFragment) sel);
						} else if (sel instanceof IPackageFragmentRoot) {
							IPackageFragmentRoot root = (IPackageFragmentRoot) sel;
							for (Iterator<String> iter= defaultPackages.iterator(); iter.hasNext();) {
								String packName= iter.next();
								IPackageFragment packageFragment = root.getPackageFragment(packName);
								if (packageFragment.exists()) {
									packageFragments.add(packageFragment);
									iter.remove();
								} else {
									checkCompletionOfDefaultPackages= true;
								}
							}
						}
					}
					ResourcesPlugin.getWorkspace().run(new StubCreationOperation(target.getLocationURI(), packageFragments), monitor);
					if (!checkCompletionOfDefaultPackages) {
						defaultPackages.clear();
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
			IProject project= ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			long end= System.currentTimeMillis();
			StringBuilder message= new StringBuilder("Took ").append((end - start) / 1000).append("s to create the stubs");
			if (!defaultPackages.isEmpty() ) {
				message.append("\n\nNo stubs generated for packages: ").append(defaultPackages.toString());
			}
			MessageDialog.openInformation(fTargetPart.getSite().getShell(), CREATE_STUBS_DIALOG_TITLE, message.toString());
		} catch (InterruptedException e) {
			// Do not log
		} catch (InvocationTargetException e) {
			JavaTestPlugin.log(e);
		} catch (CoreException exception) {
			JavaTestPlugin.log(exception);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// Do nothing
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fTargetPart= targetPart;
	}
}