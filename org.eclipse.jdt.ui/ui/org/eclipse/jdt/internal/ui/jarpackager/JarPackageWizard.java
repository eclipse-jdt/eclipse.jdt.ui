/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;

import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.jarpackager.IJarExportRunnable;
import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.dialogs.ProblemDialog;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Wizard for exporting resources from the workspace to a Java Archive (JAR) file.
 * <p>
 * This class may be instantiated and used without further configuration;
 * this class is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * <pre>
 * IWizard wizard = new JarPackageWizard();
 * wizard.init(workbench, selection);
 * WizardDialog dialog = new WizardDialog(shell, wizard);
 * dialog.open();
 * </pre>
 * During the call to <code>open</code>, the wizard dialog is presented to the
 * user. When the user hits Finish, the user-selected workspace resources 
 * are exported to the user-specified zip file, the dialog closes, and the call
 * to <code>open</code> returns.
 * </p>
 */
public class JarPackageWizard extends Wizard implements IExportWizard {
	
	private static String DIALOG_SETTINGS_KEY= "JarPackageWizard"; //$NON-NLS-1$
	
	private IStructuredSelection fSelection;
	private JarPackageData fJarPackage;
	private JarPackageWizardPage fJarPackageWizardPage;
	private boolean fHasNewDialogSettings;
	private boolean fInitializeFromJarPackage;
	
	/**
	 * Creates a wizard for exporting workspace resources to a JAR file.
	 */
	public JarPackageWizard() {
		IDialogSettings workbenchSettings= JavaPlugin.getDefault().getDialogSettings();
		IDialogSettings section= workbenchSettings.getSection(DIALOG_SETTINGS_KEY); //$NON-NLS-1$
		if (section == null)
			fHasNewDialogSettings= true;
		else {
			fHasNewDialogSettings= false;
			setDialogSettings(section);
		}
	}

	/*
	 * (non-Javadoc)
	 * Method declared on IWizard.
	 */
	public void addPages() {
		super.addPages();
		fJarPackageWizardPage= new JarPackageWizardPage(fJarPackage, fSelection);
		addPage(fJarPackageWizardPage);
		addPage(new JarOptionsPage(fJarPackage));
		addPage(new JarManifestWizardPage(fJarPackage));
	}

	/*
	 * (non-Javadoc)
	 * Method declared on IWorkbenchWizard.
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// ignore the selection argument since the main export wizard changed it
		fSelection= getValidSelection();
		fJarPackage= new JarPackageData();
		setInitializeFromJarPackage(false);
		setWindowTitle(JarPackagerMessages.getString("JarPackageWizard.windowTitle")); //$NON-NLS-1$
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAR_PACKAGER);
		setNeedsProgressMonitor(true);
	}

	/**
	 * Initializes this wizard from the given JAR package description.
	 * 
	 * @param workbench	the workbench which launched this wizard
	 * @param jarPackage the JAR package description used to initialize this wizard
	 */
	public void init(IWorkbench workbench, JarPackageData jarPackage) {
		Assert.isNotNull(workbench);
		Assert.isNotNull(jarPackage);		
		fJarPackage= jarPackage;
		setInitializeFromJarPackage(true);
		fSelection= new StructuredSelection(fJarPackage.getElements());
		setWindowTitle(JarPackagerMessages.getString("JarPackageWizard.windowTitle")); //$NON-NLS-1$
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_JAR_PACKAGER);
		setNeedsProgressMonitor(true);
	}

	/*
	 * (non-Javadoc)
	 * Method declared on IWizard.
	 */
	public boolean performFinish() {
		/*
		 * Compute and assign the minmal export list
		 */
		fJarPackage.setElements(fJarPackageWizardPage.getSelectedElementsWithoutContainedChildren());

		if (!executeExportOperation(fJarPackage.createJarExportRunnable(getShell())))
			return false;
		
		// Save the dialog settings
		if (fHasNewDialogSettings) {
			IDialogSettings workbenchSettings= JavaPlugin.getDefault().getDialogSettings();
			IDialogSettings section= workbenchSettings.getSection(DIALOG_SETTINGS_KEY);
			section= workbenchSettings.addNewSection(DIALOG_SETTINGS_KEY);
			setDialogSettings(section);
		}		
		IWizardPage[] pages= getPages();
		for (int i= 0; i < getPageCount(); i++) {
			IWizardPage page= pages[i];
			if (page instanceof IJarPackageWizardPage)
				((IJarPackageWizardPage)page).finish();
		}
		return true;
	}

	/**
	 * Exports the JAR package.
	 *
	 * @return	a boolean indicating success or failure
	 */
	protected boolean executeExportOperation(IJarExportRunnable op) {
		try {
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException ex) {
			if (ex.getTargetException() != null) {
				ExceptionHandler.handle(ex, getShell(), JarPackagerMessages.getString("JarPackageWizard.jarExportError.title"), JarPackagerMessages.getString("JarPackageWizard.jarExportError.message")); //$NON-NLS-2$ //$NON-NLS-1$
				return false;
			}
		}
		IStatus status= op.getStatus();
		if (!status.isOK()) {
			ProblemDialog.open(getShell(), JarPackagerMessages.getString("JarPackageWizard.jarExport.title"), null, status); //$NON-NLS-1$
			return !(status.matches(IStatus.ERROR));
		}
		return true;
	}

	/**
	 * Gets the current workspace page selection and converts it to a valid
	 * selection for this wizard:
	 * - resources and projects are OK
	 * - CUs are OK
	 * - Java projects are OK
	 * - Source package fragments and source packages fragement roots are ok
	 * - Java elements below a CU are converted to their CU
	 * - all other input elements are ignored
	 * 
	 * @return a valid structured selection based on the current selection
	 */
	protected IStructuredSelection getValidSelection() {
		ISelection currentSelection= JavaPlugin.getActiveWorkbenchWindow().getSelectionService().getSelection();
		if (currentSelection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection)currentSelection;
			List selectedElements= new ArrayList(structuredSelection.size());
			Iterator iter= structuredSelection.iterator();
			while (iter.hasNext()) {
				Object selectedElement=  iter.next();
				if (selectedElement instanceof IProject)
					addProject(selectedElements, (IProject)selectedElement);
				else if (selectedElement instanceof IResource)
					addResource(selectedElements, (IResource)selectedElement);
				else if (selectedElement instanceof IJavaElement)
					addJavaElement(selectedElements, (IJavaElement)selectedElement);
			}
			return new StructuredSelection(selectedElements);
		}
		else
			return StructuredSelection.EMPTY;
	}

	private void addResource(List selectedElements, IResource resource) {
		IJavaElement je= JavaCore.create(resource);
		if (je != null && je.exists() && je.getElementType() == IJavaElement.COMPILATION_UNIT)
			selectedElements.add(je);
		else
			selectedElements.add(resource);
	}

	private void addProject(List selectedElements, IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID))
				selectedElements.add(JavaCore.create(project));
		} catch (CoreException ex) {
			// ignore selected element
		}
	}

	private void addJavaElement(List selectedElements, IJavaElement je) {
		if (je.getElementType() == IJavaElement.COMPILATION_UNIT)
			selectedElements.add(je);
		else if (je.getElementType() == IJavaElement.CLASS_FILE)
			selectedElements.add(je);
		else if (je.getElementType() == IJavaElement.JAVA_PROJECT)
			selectedElements.add(je);
		else if (je.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
			if (!JavaModelUtil.getPackageFragmentRoot(je).isArchive())
				selectedElements.add(je);
		}
		else if (je.getElementType() == IJavaElement.PACKAGE_FRAGMENT_ROOT) {
			if (!((IPackageFragmentRoot)je).isArchive())
					selectedElements.add(je);
		}
		else {
			IOpenable openable= je.getOpenable();
			if (openable instanceof ICompilationUnit)
				selectedElements.add(JavaModelUtil.toOriginal((ICompilationUnit) openable));
			else if (openable instanceof IClassFile && !JavaModelUtil.getPackageFragmentRoot(je).isArchive())
				selectedElements.add(openable);
		}
	}

	boolean isInitializingFromJarPackage() {
		return fInitializeFromJarPackage;
	}

	void setInitializeFromJarPackage(boolean state) {
		fInitializeFromJarPackage= state;
	}
}
