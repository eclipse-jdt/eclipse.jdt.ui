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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.ui.preferences.ScopedPreferenceStore;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * @since 3.1
 */
public final class ProjectTemplateStore {
	
	private static final String KEY= "org.eclipse.jdt.ui.text.custom_code_templates"; //$NON-NLS-1$

	private final TemplateStore fInstanceStore;
	private final TemplateStore fProjectStore;

	private final Map fDatas= new HashMap();
	
	public ProjectTemplateStore(IProject project) {
		fInstanceStore= JavaPlugin.getDefault().getCodeTemplateStore();
		if (project == null) {
			fProjectStore= null;
		} else {
			IPreferenceStore projectSettings= new ScopedPreferenceStore(new ProjectScope(project), JavaUI.ID_PLUGIN);
			fProjectStore= new TemplateStore(projectSettings, KEY);
		}
	}
	
	public TemplatePersistenceData[] getTemplateData() {
		if (fProjectStore != null)
			return (TemplatePersistenceData[]) fDatas.values().toArray(new TemplatePersistenceData[fDatas.size()]);
		else
			return fInstanceStore.getTemplateData(false);
	}
	
	public Template findTemplateById(String id) {
		Template template= null;
		if (fProjectStore != null)
			template= fProjectStore.findTemplateById(id);
		if (template == null)
			template= fInstanceStore.findTemplateById(id);
		
		return template;
	}
	
	public void load() throws IOException {
		if (fProjectStore != null) {
			fDatas.clear();
			TemplatePersistenceData[] templateData= fInstanceStore.getTemplateData(false);
			for (int i= 0; i < templateData.length; i++) {
				final TemplatePersistenceData original= templateData[i];
				final String id= original.getId();
				if (id != null) {
					TemplatePersistenceData copy= new TemplatePersistenceData(original.getTemplate(), original.isEnabled(), id);
					fDatas.put(id, copy);
				}
			}

			TemplatePersistenceData[] ds= fProjectStore.getTemplateData(false);
			for (int i= 0; i < ds.length; i++) {
				final TemplatePersistenceData original= ds[i];
				String id= original.getId();
				if (id != null && !original.isDeleted())
					fDatas.put(id, original);
			}
		}
	}
	
	public boolean isProjectSpecific(TemplatePersistenceData data) {
		if (fProjectStore == null)
			return false;
		
		return data.isDeleted() || fProjectStore.findTemplateById(data.getId()) == null;
	}
	
	
	public void setProjectSpecific(TemplatePersistenceData data, boolean projectSpecific) {
		Assert.isNotNull(fProjectStore);
		
		if (projectSpecific) {
			if (fProjectStore.findTemplateById(data.getId()) == null)
				fProjectStore.add(data);
			data.setDeleted(false);
		} else {
			data.setDeleted(true);
		}
	}

	public void restoreDefaults() {
		if (fProjectStore == null) {
			fInstanceStore.restoreDefaults();
		} else {
			try {
				load();
			} catch (IOException e) {
				JavaPlugin.log(e);
			}
		}
	}
	public void save() throws IOException {
		if (fProjectStore == null) {
			fInstanceStore.save();
		} else {
			fProjectStore.save();
		}
	}
}
