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
package org.eclipse.jdt.internal.corext.template.java;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateMessages;
import org.eclipse.jface.text.templates.persistence.TemplateSet;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * <code>Templates</code> gives access to the available templates.
 */
public class Templates extends TemplateSet {

	private static final String DEFAULT_FILE= "default-templates.xml"; //$NON-NLS-1$
	private static final String TEMPLATE_FILE= "templates.xml"; //$NON-NLS-1$

	/** Singleton. */
	private static Templates fgTemplates;

	/**
	 * Returns an instance of templates.
	 */
	public static Templates getInstance() {
		if (fgTemplates == null)
			fgTemplates= new Templates();
		
		return fgTemplates;
	}
	
	public Templates() {
		super("template"); //$NON-NLS-1$
		create();
	}		

	private void create() {

		// TODO remove once we have a contribution mechanism
		if (ContextTypeRegistry.getInstance().getContextType("java") == null)
			ContextTypeRegistry.getInstance().add(new JavaContextType());
		if (ContextTypeRegistry.getInstance().getContextType("javadoc") == null)
			ContextTypeRegistry.getInstance().add(new JavaDocContextType());
		
		try {
			File templateFile= getTemplateFile();
			if (templateFile.exists()) {
				addFromFile(templateFile, true);
			} else {
				addFromStream(getDefaultsAsStream(), true, true);
				saveToFile(templateFile);
			}

		} catch (CoreException e) {
//			JavaPlugin.log(e);
			ErrorDialog.openError(null,
				TemplateMessages.getString("Templates.error.title"), //$NON-NLS-1$
				e.getMessage(), e.getStatus());

			clear();
		}

	}	
	
	/**
	 * Resets the template set.
	 */
	public void reset() throws CoreException {
		clear();
		addFromFile(getTemplateFile(), true);
	}

	/**
	 * Resets the template set with the default templates.
	 */
	public void restoreDefaults() throws CoreException {
		clear();
		addFromStream(getDefaultsAsStream(), true, true);
	}

	/**
	 * Saves the template set.
	 */
	public void save() throws CoreException {					
		saveToFile(getTemplateFile());
	}

	private static InputStream getDefaultsAsStream() {
		return Templates.class.getResourceAsStream(DEFAULT_FILE);
	}

	private static File getTemplateFile() {
		IPath path= JavaPlugin.getDefault().getStateLocation();
		path= path.append(TEMPLATE_FILE);
		
		return path.toFile();
	}
}

