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
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateReaderWriter;
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
	
	public ProjectTemplateStore(IProject project) {
		fInstanceStore= JavaPlugin.getDefault().getCodeTemplateStore();
		if (project == null) {
			fProjectStore= null;
		} else {
			final IPreferenceStore projectSettings= new ScopedPreferenceStore(new ProjectScope(project), JavaUI.ID_PLUGIN);
			fProjectStore= new TemplateStore(projectSettings, KEY) {
				/*
				 * Make sure we keep the id of added code templates - add removes
				 * it in the usual add() method
				 */
				public void add(TemplatePersistenceData data) {
					internalAdd(data);
				}
				
				public void save() throws IOException {
					
					StringWriter output= new StringWriter();
					TemplateReaderWriter writer= new TemplateReaderWriter();
					writer.save(getTemplateData(false), output);
					
					projectSettings.setValue(KEY, output.toString());
				}
			};
		}
	}
	
	public TemplatePersistenceData[] getTemplateData() {
		Map datas= new HashMap();
		if (fProjectStore != null) {
			TemplatePersistenceData[] data= fInstanceStore.getTemplateData(false);
			for (int i= 0; i < data.length; i++) {
				datas.put(data[i].getId(), data[i]);
			}
			data= fProjectStore.getTemplateData(false);
			for (int i= 0; i < data.length; i++) {
				datas.put(data[i].getId(), data[i]);
			}
			return (TemplatePersistenceData[]) datas.values().toArray(new TemplatePersistenceData[datas.values().size()]);
		} else {
			return fInstanceStore.getTemplateData(false);
		}
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
			fProjectStore.load();
		}
	}
	
	public boolean isProjectSpecific(String id) {
		if (fProjectStore == null)
			return false;
		
		return fProjectStore.findTemplateById(id) != null;
	}
	
	
	public void setProjectSpecific(String id, boolean projectSpecific) {
		Assert.isNotNull(fProjectStore);
		
		TemplatePersistenceData data= fProjectStore.getTemplateData(id);
		if (projectSpecific) {
			if (data == null) {
				TemplatePersistenceData orig= fInstanceStore.getTemplateData(id);
				if (orig == null)
					return; // does not exist
				TemplatePersistenceData copy= new TemplatePersistenceData(new Template(orig.getTemplate()), orig.isEnabled(), orig.getId());
				fProjectStore.add(copy);
			} else {
				data.setDeleted(false);
			}
		} else {
			if (data != null)
				data.setDeleted(true);
			// else: nothing to do
		}
	}

	public void restoreDefaults() {
		if (fProjectStore == null) {
			fInstanceStore.restoreDefaults();
		} else {
			fProjectStore.restoreDefaults();
		}
	}
	
	public void save() throws IOException {
		if (fProjectStore == null) {
			fInstanceStore.save();
		} else {
			fProjectStore.save();
		}
	}
	
	public void revertChanges() throws IOException {
		if (fProjectStore != null) {
			// nothing to do
		} else {
			fInstanceStore.load();
		}
	}
}
