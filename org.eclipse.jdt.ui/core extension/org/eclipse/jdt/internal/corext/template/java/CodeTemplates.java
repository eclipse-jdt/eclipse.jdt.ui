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
import java.util.ResourceBundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplateSet;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * <code>CodeTemplates</code> gives access to the available code templates.
 */
public class CodeTemplates extends TemplateSet {

	private static final String DEFAULT_FILE= "default-codetemplates.xml"; //$NON-NLS-1$
	private static final String TEMPLATE_FILE= "codetemplates.xml"; //$NON-NLS-1$
	private static final ResourceBundle fgResourceBundle= ResourceBundle.getBundle(JavaTemplateMessages.class.getName());

	public static final String COMMENT_SUFFIX= "comment"; //$NON-NLS-1$

	public static final String CATCHBLOCK= "catchblock"; //$NON-NLS-1$
	public static final String METHODSTUB= "methodbody"; //$NON-NLS-1$	
	public static final String NEWTYPE= "newtype"; //$NON-NLS-1$	
	public static final String CONSTRUCTORSTUB= "constructorbody"; //$NON-NLS-1$
	public static final String GETTERSTUB= "getterbody"; //$NON-NLS-1$
	public static final String SETTERSTUB= "setterbody"; //$NON-NLS-1$
	public static final String TYPECOMMENT= "type" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String FIELDCOMMENT= "field" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String METHODCOMMENT= "method" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String CONSTRUCTORCOMMENT= "constructor" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String OVERRIDECOMMENT= "override" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String GETTERCOMMENT= "getter" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String SETTERCOMMENT= "setter" + COMMENT_SUFFIX; //$NON-NLS-1$
	
	/** Singleton. */
	private static CodeTemplates fgTemplates;

	public static Template getCodeTemplate(String name) {
		return getInstance().getFirstTemplate(name);
	}

	/**
	 * Returns an instance of templates.
	 */
	public static CodeTemplates getInstance() {
		if (fgTemplates == null)
			fgTemplates= new CodeTemplates();
		
		return fgTemplates;
	}
	
	private CodeTemplates() {
		super("codetemplate"); //$NON-NLS-1$
		create();
	}
	
	private void create() {
		// TODO remove once we have a contribution mechanism
		if (ContextTypeRegistry.getInstance().getContextType("java") == null)
			ContextTypeRegistry.getInstance().add(new JavaContextType());
		CodeTemplateContextType.registerContextTypes(ContextTypeRegistry.getInstance());
		if (ContextTypeRegistry.getInstance().getContextType("javadoc") == null)
			ContextTypeRegistry.getInstance().add(new JavaDocContextType());
		
		try {
			addFromStream(getDefaultsAsStream(), false, true, fgResourceBundle);
			File templateFile= getTemplateFile();
			if (templateFile.exists()) {
				addFromFile(templateFile, false);
			}
			saveToFile(templateFile);

		} catch (CoreException e) {
			JavaPlugin.log(e);
			ErrorDialog.openError(null,
				JavaTemplateMessages.getString("CodeTemplates.error.title"), //$NON-NLS-1$
				e.getMessage(), e.getStatus());

			clear();
		}

	}	
	
	/**
	 * Resets the template set.
	 */
	public void reset() throws CoreException {
		clear();
		addFromFile(getTemplateFile(), false);
	}

	/**
	 * Resets the template set with the default templates.
	 */
	public void restoreDefaults() throws CoreException {
		clear();
		addFromStream(getDefaultsAsStream(), false, true, fgResourceBundle);
	}

	/**
	 * Saves the template set.
	 */
	public void save() throws CoreException {					
		saveToFile(getTemplateFile());
	}

	private static InputStream getDefaultsAsStream() {
		return CodeTemplates.class.getResourceAsStream(DEFAULT_FILE);
	}

	private static File getTemplateFile() {
		IPath path= JavaPlugin.getDefault().getStateLocation();
		path= path.append(TEMPLATE_FILE);
		
		return path.toFile();
	}

}
