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

	public ProjectTemplateStore(IProject project) {
		fParent= JavaPlugin.getDefault().getCodeTemplateStore();
		fPreferences= new ProjectScope(project).getNode(JavaUI.ID_PLUGIN);
	}
	
	public TemplatePersistenceData[] getTemplateData(boolean includeDeleted, boolean inherit) {
		TemplatePersistenceData[] templateData= fParent.getTemplateData(false);
		if (fPreferences == null)
			return templateData;
		
		Map datas= new HashMap();
		for (int i= 0; i < templateData.length; i++) {
			final String id= templateData[i].getId();
			if (id != null)
				datas.put(id, templateData[i]);
		}
		
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

}
