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
package org.eclipse.jdt.internal.ui.viewsupport;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * @since 3.1
 */
public final class ProjectTemplateStore {

	private final TemplateStore fParent;
	private final IEclipsePreferences fPreferences;
	private static final String fKey= "org.eclipse.jdt.ui.text.custom_code_templates"; //$NON-NLS-1$
	
	private static final class InheritedTemplatePersistenceData extends TemplatePersistenceData {

		public InheritedTemplatePersistenceData(Template template, boolean enabled, String id) {
			super(template, enabled, id);
		}
	}

	public ProjectTemplateStore(IProject project) {
		fParent= JavaPlugin.getDefault().getCodeTemplateStore();
		fPreferences= new ProjectScope(project).getNode(JavaUI.ID_PLUGIN);
	}
	
	/**
	 * Returns the set of project specific or inherited templates. Inherited
	 * ones are copied if <code>inherit</code> is <code>true</code>.
	 * 
	 * @param includeDeleted
	 * @param inherit
	 * @return the set of project specific or inherited templates
	 */
	public TemplatePersistenceData[] getTemplateData(boolean includeDeleted, boolean inherit) {
		Map datas= null;
		if (inherit) {
			TemplatePersistenceData[] templateData= fParent.getTemplateData(false);
			if (fPreferences == null)
				return templateData;
			
			datas= new HashMap();
			for (int i= 0; i < templateData.length; i++) {
				final TemplatePersistenceData original= templateData[i];
				final String id= original.getId();
				if (id != null) {
					TemplatePersistenceData copy= new InheritedTemplatePersistenceData(original.getTemplate(), original.isEnabled(), id);
					datas.put(id, copy);
				}
			}
		}
		
		if (datas == null)
			datas= new HashMap();
		
		String pref= fPreferences.get(fKey, null);
		if (pref != null && pref.trim().length() > 0) {
			Reader input= new StringReader(pref);
			TemplateReaderWriter reader= new TemplateReaderWriter();
			TemplatePersistenceData[] ds;
			try {
				ds= reader.read(input);
				for (int i= 0; i < ds.length; i++) {
					String id= ds[i].getId();
					if (id != null)
						datas.put(id, ds[i].getTemplate());
				}
			} catch (IOException e) {
				JavaPlugin.log(e);
			}
		}
		
		return (TemplatePersistenceData[]) datas.values().toArray(new TemplatePersistenceData[datas.values().size()]);
	}
	
	public boolean isInherited(TemplatePersistenceData data) {
		return data instanceof InheritedTemplatePersistenceData;
	}
	
	public Template findTemplateById(String id) {
		String projectSetting= fPreferences.get(fKey, null);
		if (projectSetting != null) {
			Template template= loadCustomTemplate(id);
			if (template != null)
				return template;
		}
		return fParent.findTemplateById(id);
	}
	
	private Template loadCustomTemplate(String id) {
		if (fPreferences == null)
			return null;
		String pref= fPreferences.get(fKey, null);
		if (pref != null && pref.trim().length() > 0) {
			Reader input= new StringReader(pref);
			try {
				TemplateReaderWriter reader= new TemplateReaderWriter();
				final TemplatePersistenceData data= reader.readSingle(input, id);
				if (data != null)
					return data.getTemplate();
			} catch (IOException e) {
				JavaPlugin.log(e);
			}
		}
		return null;
	}

	public void store(TemplatePersistenceData[] datas) {
		StringWriter output= new StringWriter();
		TemplateReaderWriter writer= new TemplateReaderWriter();
		try {
			writer.save(datas, output);
		} catch (IOException e) {
			JavaPlugin.log(e);
		}
		
		fPreferences.put(fKey, output.toString());
	}

	/**
	 * @throws IOException
	 */
	public void load() throws IOException {
		fParent.load();
	}
	/**
	 * 
	 */
	public void restoreDefaults() {
		fParent.restoreDefaults();
	}
	/**
	 * @throws IOException
	 */
	public void save() throws IOException {
		fParent.save();
	}
}
