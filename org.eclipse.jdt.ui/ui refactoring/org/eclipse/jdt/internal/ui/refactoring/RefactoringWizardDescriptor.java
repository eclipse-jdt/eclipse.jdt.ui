/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;


public class RefactoringWizardDescriptor extends AbstractDescriptor {

	private static final String EXT_ID= "refactoringWizards"; //$NON-NLS-1$
	
	public RefactoringWizardDescriptor(IConfigurationElement element) {
		super(element);
	}

	private static DescriptorManager fgDescriptions= new DescriptorManager(EXT_ID) {
		protected AbstractDescriptor createDescriptor(IConfigurationElement element) {
			return new RefactoringWizardDescriptor(element);
		}
	};
	
	public static RefactoringWizardDescriptor get(Object element) throws CoreException {
		return (RefactoringWizardDescriptor)fgDescriptions.getDescriptor(element);
	}

	public RefactoringWizard createWizard() throws CoreException {
		return (RefactoringWizard)fConfigurationElement.createExecutableExtension(CLASS);
	}

}
