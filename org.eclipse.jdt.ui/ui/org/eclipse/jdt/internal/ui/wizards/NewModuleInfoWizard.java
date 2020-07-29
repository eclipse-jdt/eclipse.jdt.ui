/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.util.InfoFilesUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.wizards.NewModuleInfoWizardPage;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class NewModuleInfoWizard extends Wizard implements INewWizard {

	private static final String TRUE= "true"; //$NON-NLS-1$

	private NewModuleInfoWizardPage fPage;

	private static final String MODULE_INFO_JAVA_FILENAME= JavaModelUtil.MODULE_INFO_JAVA;

	private IPackageFragmentRoot fTargetPkgFragmentRoot;

	private IPackageFragmentRoot[] fPackageFragmentRoots;

	private IJavaProject fProject;

	public NewModuleInfoWizard(IJavaProject project, IPackageFragmentRoot[] packageFragmentRoots, IPackageFragmentRoot targetPkgFragmentRoot) {
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWMODULE);
		setDialogSettings(JavaPlugin.getDefault().getDialogSettings());
		setWindowTitle(NewWizardMessages.NewModuleInfoWizard_title);
		fProject= project;
		fPackageFragmentRoots= packageFragmentRoots;
		if (targetPkgFragmentRoot != null) {
			fTargetPkgFragmentRoot= targetPkgFragmentRoot;
		} else if (fPackageFragmentRoots != null && fPackageFragmentRoots.length > 0) {
			fTargetPkgFragmentRoot= fPackageFragmentRoots[0];
		}
	}

	@Override
	public void addPages() {
		super.addPages();
		if (fPage == null) {
			fPage= new NewModuleInfoWizardPage();
			fPage.setWizard(this);
			fPage.init(fProject);
		}
		addPage(fPage);
	}

	@Override
	public boolean performFinish() {
		IStatus status= fPage.getModuleNameStatus();
		int severity= status.getSeverity();
		if (severity == IStatus.OK || severity == IStatus.WARNING || severity == IStatus.INFO) {
			if (fProject != null && fTargetPkgFragmentRoot != null && fPackageFragmentRoots != null && fPackageFragmentRoots.length > 0) {
				try {
					createAndOpenFile(fTargetPkgFragmentRoot, fPackageFragmentRoots);
					new Job(Messages.format(NewWizardMessages.NewModuleInfoWizard_updateProject_job, this.fProject.getElementName())) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							try {
								convertClasspathToModulePath(monitor);
							} catch (CoreException e) {
								return e.getStatus();
							}
							return Status.OK_STATUS;
						}
					}.schedule();
				} catch (CoreException e) {
					JavaPlugin.log(e);
				}
			}
		}
		return true;
	}

	private void createAndOpenFile(IPackageFragmentRoot targetPkgFragmentRoot, IPackageFragmentRoot[] packageFragmentRoots) throws CoreException {
		createModuleInfoJava(targetPkgFragmentRoot, packageFragmentRoots);

		IFile file= ((IFolder) targetPkgFragmentRoot.getCorrespondingResource()).getFile(MODULE_INFO_JAVA_FILENAME);
		if (file.exists()) {
			BasicNewResourceWizard.selectAndReveal(file, JavaPlugin.getActiveWorkbenchWindow());
			openFile(file);
		}
	}

	private void openFile(final IFile file) {
		final IWorkbenchPage activePage= JavaPlugin.getActivePage();
		if (activePage != null) {
			final Display display= getDisplay();
			if (display != null) {
				display.asyncExec(() -> {
					try {
						IDE.openEditor(activePage, file, true);
					} catch (PartInitException e) {
						JavaPlugin.log(e);
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

	private void createModuleInfoJava(IPackageFragmentRoot targetPkgFragmentRoot, IPackageFragmentRoot[] packageFragmentRoots) throws CoreException {
		String fileContent= getModuleInfoFileContent(packageFragmentRoots);
		IPackageFragment defaultPkg= targetPkgFragmentRoot.getPackageFragment(""); //$NON-NLS-1$
		InfoFilesUtil.createInfoJavaFile(MODULE_INFO_JAVA_FILENAME, fileContent.toString(), defaultPkg, fPage.isAddComments(), new NullProgressMonitor());
	}

	private String getModuleInfoFileContent(IPackageFragmentRoot[] packageFragmentRoots) throws CoreException {
		HashSet<String> exportedPackages= new HashSet<>();
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

		String[] requiredModules= JavaCore.getReferencedModules(fProject);
		String moduleName= fPage.getModuleNameText();
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

	private void convertClasspathToModulePath(IProgressMonitor monitor) throws JavaModelException {
		boolean changed= false;
		IClasspathEntry[] rawClasspath= this.fProject.getRawClasspath();
		for (int i= 0; i < rawClasspath.length; i++) {
			IClasspathEntry entry= rawClasspath[i];
			switch (entry.getEntryKind()) {
				case IClasspathEntry.CPE_CONTAINER:
				case IClasspathEntry.CPE_LIBRARY:
				case IClasspathEntry.CPE_PROJECT:
					IClasspathAttribute[] newAttributes= addModuleAttributeIfNeeded(entry.getExtraAttributes());
					if (newAttributes != null) {
						rawClasspath[i]= addAttributes(entry, newAttributes);
						changed= true;
					}
					break;
				default:
					// other kinds are not handled
			}
		}
		if (changed) {
			this.fProject.setRawClasspath(rawClasspath, monitor);
		}
	}

	private IClasspathAttribute[] addModuleAttributeIfNeeded(IClasspathAttribute[] extraAttributes) {
		for (int j= 0; j < extraAttributes.length; j++) {
			IClasspathAttribute classpathAttribute= extraAttributes[j];
			if (IClasspathAttribute.MODULE.equals(classpathAttribute.getName())) {
				if (TRUE.equals(classpathAttribute.getValue())) {
					return null; // no change required
				}
				extraAttributes[j]= JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, TRUE);
				return extraAttributes;
			}
		}
		extraAttributes= Arrays.copyOf(extraAttributes, extraAttributes.length+1);
		extraAttributes[extraAttributes.length-1]= JavaCore.newClasspathAttribute(IClasspathAttribute.MODULE, TRUE);
		return extraAttributes;
	}

	private IClasspathEntry addAttributes(IClasspathEntry entry, IClasspathAttribute[] extraAttributes) {
		switch (entry.getEntryKind()) {
			case IClasspathEntry.CPE_CONTAINER:
				return JavaCore.newContainerEntry(entry.getPath(), entry.getAccessRules(), extraAttributes, entry.isExported());
			case IClasspathEntry.CPE_LIBRARY:
				return JavaCore.newLibraryEntry(entry.getPath(), entry.getSourceAttachmentPath(),
						entry.getSourceAttachmentRootPath(), entry.getAccessRules(), extraAttributes, entry.isExported());
			case IClasspathEntry.CPE_PROJECT:
				return JavaCore.newProjectEntry(entry.getPath(), entry.getAccessRules(), entry.combineAccessRules(),
						extraAttributes, entry.isExported());
			default:
				return entry; // other kinds are not handled
		}
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		//do nothing
	}

}
