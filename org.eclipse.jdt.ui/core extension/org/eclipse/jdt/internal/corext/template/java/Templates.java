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
package org.eclipse.jdt.internal.corext.template.java;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * <code>Templates</code> gives access to the available templates.
 * 
 * @deprecated As of 3.0, replaced by {@link org.eclipse.jface.text.templates.persistence.TemplateStore}
 */
public class Templates extends org.eclipse.jdt.internal.corext.template.java.TemplateSet {

	private static final String DEFAULT_FILE= "default-templates.xml"; //$NON-NLS-1$
	private static final String TEMPLATE_FILE= "templates.xml"; //$NON-NLS-1$
	private static final ResourceBundle fgResourceBundle= ResourceBundle.getBundle(JavaTemplateMessages.class.getName());

	/** Singleton. */
	private static Templates fgTemplates;

	/**
	 * Returns an instance of templates.
	 * 
	 * @return an instance of templates
	 * @deprecated As of 3.0, replaced by
	 *             {@link org.eclipse.jdt.internal.ui.JavaPlugin#getTemplateStore()}
	 */
	public static Templates getInstance() {
		if (fgTemplates == null)
			fgTemplates= new Templates();
		
		return fgTemplates;
	}
	
	public Templates() {
		super("template", JavaPlugin.getDefault().getTemplateContextRegistry()); //$NON-NLS-1$
		create();
	}
	

	private void create() {

		try {
			File templateFile= getTemplateFile();
			if (templateFile.exists()) {
				addFromFile(templateFile, true, fgResourceBundle);
			}

		} catch (CoreException e) {
			JavaPlugin.log(e);
			clear();
		}

	}	
	
	/**
	 * Resets the template set.
	 * 
	 * @throws CoreException in case the reset operation fails
	 */
	public void reset() throws CoreException {
		clear();
		addFromFile(getTemplateFile(), true, fgResourceBundle);
	}

	/**
	 * Resets the template set with the default templates.
	 * 
	 * @throws CoreException in case the restore operation fails
	 */
	public void restoreDefaults() throws CoreException {
		clear();
		InputStream stream= getDefaultsAsStream();
		try {
			addFromStream(stream, true, true, fgResourceBundle);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException x) {
			}
		}
	}

	/**
	 * Saves the template set.
	 * 
	 * @throws CoreException in case the save operation fails
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

