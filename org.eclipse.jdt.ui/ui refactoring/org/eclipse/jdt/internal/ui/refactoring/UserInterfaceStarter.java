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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;
import org.eclipse.jdt.internal.corext.refactoring.participants.IRefactoringProcessor;

import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;

/**
 * Opens the user interface for a given refactoring.
 */
public class UserInterfaceStarter {
	
	protected static final String WIZARD= "wizard"; //$NON-NLS-1$
	
	private IConfigurationElement fConfigElement;
	
	/**
	 * Opens the user interface for the given refactoring. The provided
	 * shell should be used as a parent shell.
	 * 
	 * @param refactoring the refactoring for which the user interface
	 *  should be opened
	 * @param parent the parent shell to be used
	 * 
	 * @exception CoreException if the user interface can't be activated
	 */
	public static void run(Refactoring refactoring, Shell parent) throws CoreException {
		run(refactoring, parent, true);
	}
	
	/**
	 * Opens the user interface for the given refactoring. The provided
	 * shell should be used as a parent shell.
	 * 
	 * @param refactoring the refactoring for which the user interface
	 *  should be opened
	 * @param parent the parent shell to be used
	 * @param forceSave <code>true<code> if saving is needed before
	 *  executing the refactoring
	 * 
	 * @exception CoreException if the user interface can't be activated
	 */
	public static void run(Refactoring refactoring, Shell parent, boolean forceSave) throws CoreException {
		IRefactoringProcessor processor= (IRefactoringProcessor)refactoring.getAdapter(IRefactoringProcessor.class);
		// TODO this should change. Either IRefactoring models Refactoring API. 
		Assert.isNotNull(processor);
		UserInterfaceDescriptor descriptor= UserInterfaceDescriptor.get(processor);
		if (descriptor != null) {
			UserInterfaceStarter starter= descriptor.create();
			starter.activate(refactoring, parent, forceSave);
		} else {
			MessageDialog.openInformation(parent, 
				refactoring.getName(), 
				"No user interface found");
		}
	}
	
	/**
	 * Initializes this user interface starter with the given
	 * configuration element.
	 * 
	 * @param element the configuration element
	 */
	public void initialize(IConfigurationElement element) {
		fConfigElement= element;		
	}
	
	/**
	 * Returns the configuration element
	 * 
	 * @return the configuration element
	 */
	protected IConfigurationElement getConfigurationElement() {
		return fConfigElement;
	}
	
	/**
	 * Actually activates the user interface. This default implementation
	 * assumes that the configuration element passed to <code>initialize
	 * </code> has an attribute wizard denoting the wizard class to be
	 * used for the given refactoring.
	 * <p>
	 * Subclasses may override to open a different user interface
	 * 
	 * @param refactoring the refactoring for which the user interface
	 *  should be opened
	 * @param parent the parent shell to be used
	 * 
	 * @exception CoreException if the user interface can't be activated
	 */
	protected void activate(Refactoring refactoring, Shell parent, boolean save) throws CoreException {
		RefactoringWizard wizard= (RefactoringWizard)fConfigElement.createExecutableExtension(WIZARD);	
		wizard.initialize(refactoring);	
		new RefactoringStarter().activate(refactoring, wizard, parent, wizard.getDefaultPageTitle(), save);
	}
}
