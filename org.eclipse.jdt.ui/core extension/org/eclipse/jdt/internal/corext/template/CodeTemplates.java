/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.dialogs.ErrorDialog;

/**
 * <code>CodeTemplates</code> gives access to the available code templates.
 */
public class CodeTemplates extends TemplateSet {

	private static final String DEFAULT_FILE= "default-codetemplates.xml"; //$NON-NLS-1$
	private static final String TEMPLATE_FILE= "codetemplates.xml"; //$NON-NLS-1$

	/** Singleton. */
	private static CodeTemplates fgTemplates;

	/**
	 * Returns an instance of templates.
	 */
	public static CodeTemplates getInstance() {
		if (fgTemplates == null)
			fgTemplates= create();
		
		return fgTemplates;
	}
	
	public CodeTemplates() {
		super("codetemplate");
	}	

	private static CodeTemplates create() {
		CodeTemplates templates= new CodeTemplates();

		try {			
			File templateFile= getTemplateFile();
			if (templateFile.exists()) {
				templates.addFromFile(templateFile);
			} else {
				templates.addFromStream(getDefaultsAsStream());
				templates.saveToFile(templateFile);
			}

		} catch (CoreException e) {
			JavaPlugin.log(e);
			ErrorDialog.openError(null,
				TemplateMessages.getString("Templates.error.title"), //$NON-NLS-1$
				e.getMessage(), e.getStatus());

			templates.clear();
		}

		return templates;
	}	
	
	/**
	 * Resets the template set.
	 */
	public void reset() throws CoreException {
		clear();
		addFromFile(getTemplateFile());
	}

	/**
	 * Resets the template set with the default templates.
	 */
	public void restoreDefaults() throws CoreException {
		clear();
		addFromStream(getDefaultsAsStream());
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

