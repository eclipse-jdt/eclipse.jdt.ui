/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.template;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import org.eclipse.jface.dialogs.ErrorDialog;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * <code>CodeTemplates</code> gives access to the available code templates.
 */
public class CodeTemplates extends TemplateSet {

	private static final String DEFAULT_FILE= "default-codetemplates.xml"; //$NON-NLS-1$
	private static final String TEMPLATE_FILE= "codetemplates.xml"; //$NON-NLS-1$

	public static final String COMMENT_SUFFIX= "comment"; //$NON-NLS-1$

	public static final String CATCHBLOCK= "catchblock"; //$NON-NLS-1$
	public static final String METHODSTUB= "methodbody"; //$NON-NLS-1$	
	public static final String NEWTYPE= "newtype"; //$NON-NLS-1$	
	public static final String CONSTRUCTORSTUB= "constructorbody"; //$NON-NLS-1$
	public static final String TYPECOMMENT= "type" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String METHODCOMMENT= "method" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String CONSTRUCTORCOMMENT= "constructor" + COMMENT_SUFFIX; //$NON-NLS-1$
	public static final String OVERRIDECOMMENT= "override" + COMMENT_SUFFIX; //$NON-NLS-1$


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
	
	public CodeTemplates() {
		super("codetemplate"); //$NON-NLS-1$
		create();
	}
	
	private void create() {
		try {
			addFromStream(getDefaultsAsStream(), false);
			File templateFile= getTemplateFile();
			if (templateFile.exists()) {
				addFromFile(templateFile, false);
			}
			saveToFile(templateFile);

		} catch (CoreException e) {
			JavaPlugin.log(e);
			ErrorDialog.openError(null,
				TemplateMessages.getString("CodeTemplates.error.title"), //$NON-NLS-1$
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
		addFromStream(getDefaultsAsStream(), false);
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

