/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.ui.wizards;

import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.core.IClasspathEntry;

/**
 * A classpath container page allows the user to create a
 * new or edit an existing classpatch container entry.
 * <p>
 * Clients should implement this interface and include the 
 * name of their class in an extension contributed to the 
 * jdt.ui's classpath container page extension point 
 * (named <code>"org.eclipse.jdt.ui.classpathContainerPage"</code>) if they 
 * want to provide a classpath container entry 
 * </p>
 * <p>
 * Clients implementing this interface may subclass from 
 * org.eclipse.jface.wizard.WizardPage.
 * </p>
 *
 * @since 2.0
 */
public interface IClasspathContainerPage extends IWizardPage {
	
	/**
	 * Called when the classpath container wizard is closed by selecting 
	 * the finish button.
	 * Implementers may store the page result (new/changed classpath 
	 * entry returned in getSelection) here.
	 * @param Return if the operation was succesful. Only when returned
	 * <code>true</code>, the wizard will close.
	 */
	public boolean finish();
	
	/**
	 * Returns the classpath container entry edited or created on the page 
	 * after the wizard has closed.
	 * If an entry was set with setSelection, the returned entry
	 * will replace this entry.
	 * The entry must be of kind IClasspathEntry.CPE_CONTAINER
	 * 
	 * @return the classpath entry edited or created on the page.
	 */
	public IClasspathEntry getSelection();
	
	/**
	 * Sets the classpath container entry to be edited or
	 * <code>null</code> if a new entry should be created.
	 * 
	 * @param containerEntry the classpath entry to edit or <code>null</code>
	 */
	public void setSelection(IClasspathEntry containerEntry);
		
}