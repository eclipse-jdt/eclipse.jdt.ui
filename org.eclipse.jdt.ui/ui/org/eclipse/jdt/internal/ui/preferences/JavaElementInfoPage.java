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
package org.eclipse.jdt.internal.ui.preferences;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.dialogs.Dialog;

import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;


/**
 * This is a dummy PropertyPage for JavaElements.
 * Copied from the ResourceInfoPage
 */
public class JavaElementInfoPage extends PropertyPage {
	
	/*
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IJavaHelpContextIds.JAVA_ELEMENT_INFO_PAGE);
	}		
	
	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		// ensure the page has no special buttons
		noDefaultAndApplyButton();

		IJavaElement element= (IJavaElement)getElement();
		
		IResource resource= element.getResource();
		
		Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		Label nameLabel= new Label(composite, SWT.NONE);
		nameLabel.setText(PreferencesMessages.getString("JavaElementInfoPage.nameLabel")); //$NON-NLS-1$

		Label nameValueLabel= new Label(composite, SWT.NONE);
		nameValueLabel.setText(element.getElementName());

		if (resource != null) {
			// path label
			Label pathLabel= new Label(composite, SWT.NONE);
			pathLabel.setText(PreferencesMessages.getString("JavaElementInfoPage.resource_path")); //$NON-NLS-1$

			// path value label
			Label pathValueLabel= new Label(composite, SWT.NONE);
			pathValueLabel.setText(resource.getFullPath().toString());
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit)element;
			Label packageLabel= new Label(composite, SWT.NONE);
			packageLabel.setText(PreferencesMessages.getString("JavaElementInfoPage.package")); //$NON-NLS-1$
			Label packageName= new Label(composite, SWT.NONE);
			packageName.setText(unit.getParent().getElementName());
			
		} else if (element instanceof IPackageFragment) {
			IPackageFragment packageFragment= (IPackageFragment)element;
			Label packageContents= new Label(composite, SWT.NONE);
			packageContents.setText(PreferencesMessages.getString("JavaElementInfoPage.package_contents")); //$NON-NLS-1$
			Label packageContentsType= new Label(composite, SWT.NONE);
			try {
				if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE) 
					packageContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.source")); //$NON-NLS-1$
				else
					packageContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.binary")); //$NON-NLS-1$
			} catch (JavaModelException e) {
				packageContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.not_present")); //$NON-NLS-1$
			}
		} else if (element instanceof IPackageFragmentRoot) {
			Label rootContents= new Label(composite, SWT.NONE);
			rootContents.setText(PreferencesMessages.getString("JavaElementInfoPage.classpath_entry_kind")); //$NON-NLS-1$
			Label rootContentsType= new Label(composite, SWT.NONE);
			try {
				IClasspathEntry entry= ((IPackageFragmentRoot)element).getRawClasspathEntry();
				if (entry != null) {
					switch (entry.getEntryKind()) {
						case IClasspathEntry.CPE_SOURCE:
							rootContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.source")); break; //$NON-NLS-1$
						case IClasspathEntry.CPE_PROJECT:
							rootContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.project")); break; //$NON-NLS-1$
						case IClasspathEntry.CPE_LIBRARY:
							rootContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.library")); break; //$NON-NLS-1$
						case IClasspathEntry.CPE_VARIABLE:
							rootContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.variable")); //$NON-NLS-1$
							Label varPath= new Label(composite, SWT.NONE);
							varPath.setText(PreferencesMessages.getString("JavaElementInfoPage.variable_path")); //$NON-NLS-1$
							Label varPathVar= new Label(composite, SWT.NONE);
							varPathVar.setText(entry.getPath().makeRelative().toString());							
							break;
					}
				} else {
					rootContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.not_present")); //$NON-NLS-1$
				}
			} catch (JavaModelException e) {
				rootContentsType.setText(PreferencesMessages.getString("JavaElementInfoPage.not_present")); //$NON-NLS-1$
			}
		} else if (element instanceof IJavaProject) {
			Label packageLabel= new Label(composite, SWT.NONE);
			packageLabel.setText(PreferencesMessages.getString("JavaElementInfoPage.location")); //$NON-NLS-1$
			IPath location= ((IJavaProject)element).getProject().getLocation();
			if (location != null) {
				Label packageName= new Label(composite, SWT.NONE);
				packageName.setText(location.toOSString());				
			}
		}
		Dialog.applyDialogFont(composite);		
		return composite;
	}

	/**
	 */
	protected boolean doOk() {
		// nothing to do - read-only page
		return true;
	}

}
