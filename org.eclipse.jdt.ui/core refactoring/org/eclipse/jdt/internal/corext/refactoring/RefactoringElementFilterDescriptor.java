/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;

import org.eclipse.jface.util.SafeRunnable;

import org.eclipse.ui.IPluginContribution;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.JavaUI;

/**
 * Represents a filter which is provided by the
 * "org.eclipse.jdt.ui.internal_refactoringElementFilter" extension point.
 * 
 * @since 3.2
 */
public class RefactoringElementFilterDescriptor implements IPluginContribution {

	private static final String EXTENSION_POINT_NAME= "internal_refactoringElementFilter"; //$NON-NLS-1$

	private static final String FILTER_TAG= "filter"; //$NON-NLS-1$

	private static final String ID_ATTRIBUTE= "id"; //$NON-NLS-1$
	private static final String CLASS_ATTRIBUTE= "class"; //$NON-NLS-1$

	private static RefactoringElementFilterDescriptor[] fgFilterDescriptors;

	private IConfigurationElement fElement;
	private RefactoringElementFilter fRefactoringElementFilter;
	private boolean fFilterOKToUse;

	public static boolean isFiltered(final ICompilationUnit cu) {
		if (cu == null)
			return false;
		
		RefactoringElementFilterDescriptor[] filterDescriptors= getFilterDescriptors();
		for (int i= 0; i < filterDescriptors.length; i++) {
			final RefactoringElementFilter filter= filterDescriptors[i].getRefactoringElementFilter();
			if (filter == null)
				continue;
			
			final boolean[] filtered= new boolean[1];
			SafeRunner.run(new SafeRunnable() {
				public void run() throws Exception {
					filtered[0]= filter.filter(cu);
				}
			});
			// TODO: disable filter if it throws an exception?
			
			if (filtered[0])
				return true;
		}
		return false;
	}
	
	private static RefactoringElementFilterDescriptor[] getFilterDescriptors() {
		if (fgFilterDescriptors == null) {
			IExtensionRegistry registry= Platform.getExtensionRegistry();
			IConfigurationElement[] elements= registry.getConfigurationElementsFor(JavaUI.ID_PLUGIN, EXTENSION_POINT_NAME);
			fgFilterDescriptors= createFilterDescriptors(elements);
		}	
		return fgFilterDescriptors;
	}
	
	private static RefactoringElementFilterDescriptor[] createFilterDescriptors(IConfigurationElement[] elements) {
		List result= new ArrayList(5);
		Set descIds= new HashSet(5);
		for (int i= 0; i < elements.length; i++) {
			final IConfigurationElement element= elements[i];
			if (FILTER_TAG.equals(element.getName())) {

				final RefactoringElementFilterDescriptor[] desc= new RefactoringElementFilterDescriptor[1];
				SafeRunner.run(new SafeRunnable(RefactoringCoreMessages.RefactoringElementFilterDescriptor_filterDescriptionCreationError_message) { 
					public void run() throws Exception {
						desc[0]= new RefactoringElementFilterDescriptor(element);
					}
				});

				if (desc[0] != null && !descIds.contains(desc[0].getId())) {
					result.add(desc[0]);
					descIds.add(desc[0].getId());
				}
			}
		}
		return (RefactoringElementFilterDescriptor[])result.toArray(new RefactoringElementFilterDescriptor[result.size()]);
	}
	
	private RefactoringElementFilterDescriptor(IConfigurationElement element) {
		fElement= element;
		Assert.isNotNull(getFilterClass(), "An extension for extension-point org.eclipse.jdt.ui.internal_refactoringElementFilter does not specify a valid class"); //$NON-NLS-1$
		Assert.isNotNull(getId(), "An extension for extension-point org.eclipse.jdt.ui.internal_refactoringElementFilter does not provide a valid ID"); //$NON-NLS-1$
		fFilterOKToUse= true;
	}

	private  RefactoringElementFilter getRefactoringElementFilter() {
		if (! fFilterOKToUse)
			return null;
		
		if (fRefactoringElementFilter != null)
			return fRefactoringElementFilter;
		
		String message= Messages.format(RefactoringCoreMessages.RefactoringElementFilterDescriptor_filterCreationError_message, getId()); 
		ISafeRunnable code= new SafeRunnable(message) {
			public void run() throws Exception {
				fRefactoringElementFilter= (RefactoringElementFilter)fElement.createExecutableExtension(CLASS_ATTRIBUTE);
			}
			public void handleException(Throwable e) {
				fFilterOKToUse= false;
				super.handleException(e);
			}
		};
		SafeRunner.run(code);
		return fRefactoringElementFilter;
	}
	
	/**
	 * @return the filter's id
	 */
	public String getId() {
		return fElement.getAttribute(ID_ATTRIBUTE);
	}
	
	/**
	 * @return the filter class
	 */
	public String getFilterClass() {
		return fElement.getAttribute(CLASS_ATTRIBUTE);
	}
	
	/*
	 * @see org.eclipse.ui.IPluginContribution#getLocalId()
	 */
	public String getLocalId() {
		return fElement.getAttribute(ID_ATTRIBUTE);
	}

    /*
     * @see org.eclipse.ui.IPluginContribution#getPluginId()
     */
    public String getPluginId() {
        return fElement.getContributor().getName();
    }
}
