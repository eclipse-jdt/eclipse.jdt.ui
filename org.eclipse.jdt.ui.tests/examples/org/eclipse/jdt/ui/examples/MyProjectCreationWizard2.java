/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

/**
<extension
	point="org.eclipse.ui.newWizards">
  	<wizard
		id="org.eclipse.jdt.ui.examples.MyProjectCreationWizard2"
		name="My Project 2"
		class="org.eclipse.jdt.ui.examples.MyProjectCreationWizard2"
		category="org.eclipse.jdt.ui.java"
		project="true"
		finalPerspective="org.eclipse.jdt.ui.JavaPerspective"
		icon="icons/full/ctool16/newjprj_wiz.gif">
		<description>My project 2</description>
    </wizard>
</extension>
 */

/**
 * This example shows how to implement an own project wizard that uses the
 * JavaCapabilityConfigurationPage to allow the user to configure the Java build path.
 */
public class MyProjectCreationWizard2 extends Wizard implements IExecutableExtension, INewWizard {

	private NewJavaProjectWizardPageOne fMainPage;
	private NewJavaProjectWizardPageTwo fJavaPage;
	private IWizardPage fExtraPage;

	private IConfigurationElement fConfigElement;


	public MyProjectCreationWizard2() {
		setWindowTitle("New ZZ Project");
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
	}

	/* (non-Javadoc)
	 * @see Wizard#addPages
	 */
	public void addPages() {
		super.addPages();

		// simplified main page: no extrenal laoctaion, no src/bin selection always create our own layout
		fMainPage= new NewJavaProjectWizardPageOne() {
			public void createControl(Composite parent) {
				initializeDialogUnits(parent);

				Composite composite= new Composite(parent, SWT.NULL);
				composite.setFont(parent.getFont());
				composite.setLayout(new GridLayout(1, false));
				composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

				// create UI elements
				Control nameControl= createNameControl(composite);
				nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				Control jreControl= createJRESelectionControl(composite);
				jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				Control workingSetControl= createWorkingSetControl(composite);
				workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				Control infoControl= createInfoControl(composite);
				infoControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

				setControl(composite);
			}

			public IClasspathEntry[] getSourceClasspathEntries() {
				IPath path1= new Path(getProjectName()).append("src").makeAbsolute();
				IPath path2= new Path(getProjectName()).append("tests").makeAbsolute();
				return new IClasspathEntry[] { JavaCore.newSourceEntry(path1), JavaCore.newSourceEntry(path2) };
			}

			public IPath getOutputLocation() {
				IPath path1= new Path(getProjectName()).append("classes").makeAbsolute();
				return path1;
			}
		};
		fMainPage.setProjectName("ZZ");

		// the main page
		addPage(fMainPage);

		// the Java build path configuration page
		fJavaPage= new NewJavaProjectWizardPageTwo(fMainPage);

		addPage(fJavaPage);

		fExtraPage= new WizardPage("My Page") {

			public void createControl(Composite parent) {
				initializeDialogUnits(parent);

				Button button= new Button(parent, SWT.CHECK);
				button.setText("Make it a special project");

				setControl(button);
			}

		};


		addPage(fExtraPage);
	}


	/* (non-Javadoc)
	 * @see Wizard#performFinish
	 */
	public boolean performFinish() {
		WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
			protected void execute(IProgressMonitor monitor) throws CoreException, InvocationTargetException, InterruptedException {
				fJavaPage.performFinish(monitor);
				// use the result from the extra page
			}
		};
		try {
			getContainer().run(false, true, op);

			IJavaProject newElement= fJavaPage.getJavaProject();

			IWorkingSet[] workingSets= fMainPage.getWorkingSets();
			if (workingSets.length > 0) {
				PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(newElement, workingSets);
			}
			BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
			BasicNewResourceWizard.selectAndReveal(newElement.getResource(), PlatformUI.getWorkbench().getActiveWorkbenchWindow());


		} catch (InvocationTargetException e) {
			return false; // TODO: should open error dialog and log
		} catch  (InterruptedException e) {
			return false; // canceled
		}
		return true;
	}

	public boolean performCancel() {
		fJavaPage.performCancel();
		return true;
	}



}
