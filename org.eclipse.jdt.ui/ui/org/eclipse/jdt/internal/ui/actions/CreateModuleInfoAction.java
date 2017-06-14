/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
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
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.util.InfoFilesUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.JavaPlugin;

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

				if (targetPkgFragmentRoot == null) {
					targetPkgFragmentRoot= packageFragmentRoots[0];
				}

				createAndOpenFile(targetPkgFragmentRoot, packageFragmentRoots);

			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
	}

	private void createAndOpenFile(IPackageFragmentRoot targetPkgFragmentRoot, IPackageFragmentRoot[] packageFragmentRoots) throws CoreException {
		createModuleInfoJava(targetPkgFragmentRoot, packageFragmentRoots);

		IFile file= ((IFolder) targetPkgFragmentRoot.getCorrespondingResource()).getFile(MODULE_INFO_JAVA_FILENAME);
		if (file.exists()) {
			BasicNewResourceWizard.selectAndReveal(file, JavaPlugin.getActiveWorkbenchWindow());
			openFile(file);
		}
	}

	private void createModuleInfoJava(IPackageFragmentRoot targetPkgFragmentRoot, IPackageFragmentRoot[] packageFragmentRoots) throws CoreException {
		String fileContent= getModuleInfoFileContent(packageFragmentRoots);
		IPackageFragment defaultPkg= targetPkgFragmentRoot.getPackageFragment(""); //$NON-NLS-1$
		InfoFilesUtil.createInfoJavaFile(MODULE_INFO_JAVA_FILENAME, fileContent.toString(), defaultPkg, new NullProgressMonitor());
	}

	private String getModuleInfoFileContent(IPackageFragmentRoot[] packageFragmentRoots) throws CoreException {
		List<String> exportedPackages= new ArrayList<>();
		for (IPackageFragmentRoot packageFragmentRoot : packageFragmentRoots) {
			for (IJavaElement child : packageFragmentRoot.getChildren()) {
				if (child instanceof IPackageFragment) {
					IPackageFragment pkgFragment= (IPackageFragment) child;
					if (!pkgFragment.isDefaultPackage() && pkgFragment.getCompilationUnits().length != 0) {
						exportedPackages.add(pkgFragment.getElementName());
					}
				}
			}
		}

		IJavaProject javaProject= packageFragmentRoots[0].getJavaProject();

		String[] requiredModules= JavaCore.getReferencedModules(javaProject);

		IModuleDescription moduleDescription= javaProject.getModuleDescription();
		String moduleName= moduleDescription != null ? moduleDescription.getElementName() : javaProject.getElementName();

		StringBuilder fileContent= new StringBuilder();
		fileContent.append("module "); //$NON-NLS-1$
		fileContent.append(moduleName);
		fileContent.append(" {"); //$NON-NLS-1$

		for (String exportedPkg : exportedPackages) {
			fileContent.append("exports "); //$NON-NLS-1$
			fileContent.append(exportedPkg);
			fileContent.append(";"); //$NON-NLS-1$
		}

		for (String requiredModule : requiredModules) {
			fileContent.append("requires "); //$NON-NLS-1$
			fileContent.append(requiredModule);
			fileContent.append(';');
		}

		fileContent.append('}');

		return fileContent.toString();
	}

	private void openFile(final IFile file) {
		final IWorkbenchPage activePage= JavaPlugin.getActivePage();
		if (activePage != null) {
			final Display display= getDisplay();
			if (display != null) {
				display.asyncExec(new Runnable() {
					@Override
					public void run() {
						try {
							IDE.openEditor(activePage, file, true);
						} catch (PartInitException e) {
							JavaPlugin.log(e);
						}
					}
				});
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
