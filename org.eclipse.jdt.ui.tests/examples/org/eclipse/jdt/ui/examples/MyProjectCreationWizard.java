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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.launching.JavaRuntime;

import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

/**
<extension
	point="org.eclipse.ui.newWizards">
  	<wizard
		id="org.eclipse.jdt.ui.examples.MyProjectCreationWizard"
		name="My project"
		class="org.eclipse.jdt.ui.examples.MyProjectCreationWizard"
		category="org.eclipse.jdt.ui.java"
		project="true"
		finalPerspective="org.eclipse.jdt.ui.JavaPerspective"
		icon="icons/full/ctool16/newjprj_wiz.gif">
		<description>My project</description>
    </wizard>
</extension>
 */

/**
 * This example shows how to implement an own project wizard that uses the
 * JavaCapabilityConfigurationPage to allow the user to configure the Java build path.
 */
public class MyProjectCreationWizard extends Wizard implements IExecutableExtension, INewWizard {

	private WizardNewProjectCreationPage fMainPage;
	private JavaCapabilityConfigurationPage fJavaPage;

	private IConfigurationElement fConfigElement;

	private IWorkbench fWorkbench;
	public MyProjectCreationWizard() {
		setWindowTitle("New XY Project");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement cfig, String propertyName, Object data) {
		//  The config element will be used in <code>finishPage</code> to set the result perspective.
		fConfigElement= cfig;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		fWorkbench= workbench;
	}

	/* (non-Javadoc)
	 * @see Wizard#addPages
	 */
	public void addPages() {
		super.addPages();
		fMainPage= new WizardNewProjectCreationPage("NewProjectCreationWizard");
		fMainPage.setTitle("New");
		fMainPage.setDescription("Create a new XY project.");

		// the main page
		addPage(fMainPage);

		// the Java build path configuration page
		fJavaPage= new JavaCapabilityConfigurationPage() {
			public void setVisible(boolean visible) {
				// need to override to react to changes on first page
				updatePage();
				super.setVisible(visible);
			}
		};
		addPage(fJavaPage);

		//	TODO: add your pages here
	}

	private void updatePage() {
		IJavaProject jproject= JavaCore.create(fMainPage.getProjectHandle());
		if (!jproject.equals(fJavaPage.getJavaProject())) {
			IClasspathEntry[] buildPath= {
				JavaCore.newSourceEntry(jproject.getPath().append("in")),
				JavaRuntime.getDefaultJREContainerEntry()
			};
			IPath outputLocation= jproject.getPath().append("out");
			fJavaPage.init(jproject, outputLocation, buildPath, false);
		}
	}

	private void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
		if (monitor == null) {
			monitor= new NullProgressMonitor();
		}
		try {
			monitor.beginTask("Creating XY project...", 3); // 3 steps

			IProject project= fMainPage.getProjectHandle();
			IPath locationPath= fMainPage.getLocationPath();

			// create the project
			IProjectDescription desc= project.getWorkspace().newProjectDescription(project.getName());
			if (!fMainPage.useDefaults()) {
				desc.setLocation(locationPath);
			}
			project.create(desc, new SubProgressMonitor(monitor, 1));
			project.open(new SubProgressMonitor(monitor, 1));

			updatePage();
			fJavaPage.configureJavaProject(new SubProgressMonitor(monitor, 1));
			// TODO: configure your page / nature

			// change to the perspective specified in the plugin.xml
			BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
			BasicNewResourceWizard.selectAndReveal(project, fWorkbench.getActiveWorkbenchWindow());

		} finally {
			monitor.done();
		}
	}

	/* (non-Javadoc)
	 * @see Wizard#performFinish
	 */
	public boolean performFinish() {
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
				finishPage(monitor);
			}
		};
		try {
			getContainer().run(false, true, op);
		} catch (InvocationTargetException e) {
			return false; // TODO: should open error dialog and log
		} catch  (InterruptedException e) {
			return false; // canceled
		}
		return true;
	}



}
