/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.preferences;

import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusUtil;
import org.eclipse.jdt.internal.ui.wizards.IStatusChangeListener;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.ArchiveFileFilter;

/**
 * Property page used to set the project's Javadoc location for sources
 */
public class JavadocConfigurationPropertyPage extends PropertyPage implements IStatusChangeListener {

	private JavadocConfigurationBlock fJavadocConfigurationBlock;
	private boolean fIsValidElement;

	public JavadocConfigurationPropertyPage() {
	}

	/**
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		IJavaElement elem= getJavaElement();
		try {
			if (elem instanceof IPackageFragmentRoot && ((IPackageFragmentRoot) elem).getKind() == IPackageFragmentRoot.K_BINARY) {
				fIsValidElement= true;
				setDescription(PreferencesMessages.getString("JavadocConfigurationPropertyPage.IsPackageFragmentRoot.description")); //$NON-NLS-1$
			} else if (elem instanceof IJavaProject) {
				fIsValidElement= true;
				setDescription(PreferencesMessages.getString("JavadocConfigurationPropertyPage.IsJavaProject.description")); //$NON-NLS-1$
			} else {
				fIsValidElement= false;
			}
		} catch (JavaModelException e) {
			fIsValidElement= false;
		}
		if (!fIsValidElement) {
			setDescription(PreferencesMessages.getString("JavadocConfigurationPropertyPage.IsIncorrectElement.description")); //$NON-NLS-1$
		}
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVADOC_CONFIGURATION_PROPERTY_PAGE);
	}

	/*
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		if (!fIsValidElement) {
			return new Composite(parent, SWT.NONE);
		}
		
		IJavaElement elem= getJavaElement();
		
		URL initialLocation= null;
		if (elem != null) {
			try {
				initialLocation= JavaUI.getJavadocBaseLocation(elem);
			} catch (JavaModelException e) {
				JavaPlugin.log(e);
			}
		}
		
		boolean isProject= (elem instanceof IJavaProject);
		fJavadocConfigurationBlock= new JavadocConfigurationBlock(getShell(), this, initialLocation, isProject);
		Control control= fJavadocConfigurationBlock.createContents(parent);
		control.setVisible(elem != null);

		Dialog.applyDialogFont(control);
		return control;
	}

	private IJavaElement getJavaElement() {
		IAdaptable adaptable= getElement();
		IJavaElement elem= (IJavaElement) adaptable.getAdapter(IJavaElement.class);
		if (elem == null) {

			IResource resource= (IResource) adaptable.getAdapter(IResource.class);
			//special case when the .jar is a file
			try {
				if (resource instanceof IFile && ArchiveFileFilter.isArchivePath(resource.getFullPath())) {
					IProject proj= resource.getProject();
					if (proj.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject jproject= JavaCore.create(proj);
						elem= jproject.getPackageFragmentRoot(resource); // create a handle
					}
				}
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		return elem;
	}

	/*
	 * @see PreferencePage#performDefaults()
	 */
	protected void performDefaults() {
		fJavadocConfigurationBlock.performDefaults();
		super.performDefaults();
	}

	/**
	 * @see org.eclipse.jface.preference.IPreferencePage#performOk()
	 */
	public boolean performOk() {
		URL javadocLocation= fJavadocConfigurationBlock.getJavadocLocation();
		IJavaElement elem= getJavaElement();
		if (elem instanceof IJavaProject) {
			JavaUI.setProjectJavadocLocation((IJavaProject) elem, javadocLocation);
		} else if (elem instanceof IPackageFragmentRoot) {
			JavaUI.setLibraryJavadocLocation(elem.getPath(), javadocLocation);
		}
		return true;
	}

	/**
	 * @see IStatusChangeListener#statusChanged(IStatus)
	 */
	public void statusChanged(IStatus status) {
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

}
