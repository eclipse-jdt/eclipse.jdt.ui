/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.ui.PlatformUI;

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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.JAVA_ELEMENT_INFO_PAGE);
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
		nameLabel.setText(PreferencesMessages.JavaElementInfoPage_nameLabel); 

		Label nameValueLabel= new Label(composite, SWT.NONE);
		nameValueLabel.setText(element.getElementName());

		if (resource != null) {
			// path label
			Label pathLabel= new Label(composite, SWT.NONE);
			pathLabel.setText(PreferencesMessages.JavaElementInfoPage_resource_path); 

			// path value label
			Label pathValueLabel= new Label(composite, SWT.NONE);
			pathValueLabel.setText(resource.getFullPath().toString());
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit unit= (ICompilationUnit)element;
			Label packageLabel= new Label(composite, SWT.NONE);
			packageLabel.setText(PreferencesMessages.JavaElementInfoPage_package); 
			Label packageName= new Label(composite, SWT.NONE);
			packageName.setText(unit.getParent().getElementName());
			
		} else if (element instanceof IPackageFragment) {
			IPackageFragment packageFragment= (IPackageFragment)element;
			Label packageContents= new Label(composite, SWT.NONE);
			packageContents.setText(PreferencesMessages.JavaElementInfoPage_package_contents); 
			Label packageContentsType= new Label(composite, SWT.NONE);
			try {
				if (packageFragment.getKind() == IPackageFragmentRoot.K_SOURCE) 
					packageContentsType.setText(PreferencesMessages.JavaElementInfoPage_source); 
				else
					packageContentsType.setText(PreferencesMessages.JavaElementInfoPage_binary); 
			} catch (JavaModelException e) {
				packageContentsType.setText(PreferencesMessages.JavaElementInfoPage_not_present); 
			}
		} else if (element instanceof IPackageFragmentRoot) {
			Label rootContents= new Label(composite, SWT.NONE);
			rootContents.setText(PreferencesMessages.JavaElementInfoPage_classpath_entry_kind); 
			Label rootContentsType= new Label(composite, SWT.NONE);
			try {
				IClasspathEntry entry= ((IPackageFragmentRoot)element).getRawClasspathEntry();
				if (entry != null) {
					switch (entry.getEntryKind()) {
						case IClasspathEntry.CPE_SOURCE:
							rootContentsType.setText(PreferencesMessages.JavaElementInfoPage_source); break; 
						case IClasspathEntry.CPE_PROJECT:
							rootContentsType.setText(PreferencesMessages.JavaElementInfoPage_project); break; 
						case IClasspathEntry.CPE_LIBRARY:
							rootContentsType.setText(PreferencesMessages.JavaElementInfoPage_library); break; 
						case IClasspathEntry.CPE_VARIABLE:
							rootContentsType.setText(PreferencesMessages.JavaElementInfoPage_variable); 
							Label varPath= new Label(composite, SWT.NONE);
							varPath.setText(PreferencesMessages.JavaElementInfoPage_variable_path); 
							Label varPathVar= new Label(composite, SWT.NONE);
							varPathVar.setText(entry.getPath().makeRelative().toString());							
							break;
					}
				} else {
					rootContentsType.setText(PreferencesMessages.JavaElementInfoPage_not_present); 
				}
			} catch (JavaModelException e) {
				rootContentsType.setText(PreferencesMessages.JavaElementInfoPage_not_present); 
			}
		} else if (element instanceof IJavaProject) {
			Label packageLabel= new Label(composite, SWT.NONE);
			packageLabel.setText(PreferencesMessages.JavaElementInfoPage_location); 
			IPath location= ((IJavaProject)element).getProject().getLocation();
			if (location != null) {
				Label packageName= new Label(composite, SWT.NONE);
				packageName.setText(location.toOSString());				
			}
		}
		Dialog.applyDialogFont(composite);		
		return composite;
	}

}
