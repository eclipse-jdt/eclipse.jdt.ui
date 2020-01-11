/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.core.manipulation;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import org.osgi.service.prefs.BackingStoreException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;

import org.eclipse.text.templates.TemplatePersistenceData;
import org.eclipse.text.templates.TemplateReaderWriter;
import org.eclipse.text.templates.TemplateStoreCore;

import org.eclipse.jface.text.templates.Template;

import org.eclipse.jdt.core.manipulation.JavaManipulation;

/**
 * @since 3.1
 */
public final class ProjectTemplateStore {

	private static final String KEY= "org.eclipse.jdt.ui.text.custom_code_templates"; //$NON-NLS-1$

	private final TemplateStoreCore fInstanceStore;
	private final TemplateStoreCore fProjectStore;

	public ProjectTemplateStore(IProject project) {
		fInstanceStore= JavaManipulation.getCodeTemplateStore();
		if (project == null) {
			fProjectStore= null;
		} else {
			final IEclipsePreferences projectSettings= new ProjectScope(project).getNode(JavaManipulation.getPreferenceNodeId());
			fProjectStore= new TemplateStoreCore(projectSettings, KEY) {
				/*
				 * Make sure we keep the id of added code templates - add removes
				 * it in the usual add() method
				 */
				@Override
				public void add(TemplatePersistenceData data) {
					internalAdd(data);
				}

				@Override
				public void save() throws IOException {

					for (TemplatePersistenceData templateData : ProjectTemplateStore.this.getTemplateData()) {
						if (isProjectSpecific(templateData.getId())) {
							StringWriter output= new StringWriter();
							TemplateReaderWriter writer= new TemplateReaderWriter();
							writer.save(getTemplateData(false), output);

							projectSettings.put(KEY, output.toString());
							try {
								projectSettings.flush();
							} catch (BackingStoreException e) {
							}

							return;
						}
					}

					// See IPreferenceStore for default String value
					projectSettings.put(KEY, ""); //$NON-NLS-1$
					try {
						projectSettings.flush();
					} catch (BackingStoreException e) {
					}
				}
			};
		}
	}

	public static boolean hasProjectSpecificTempates(IProject project) {
		String pref= new ProjectScope(project).getNode(JavaManipulation.getPreferenceNodeId()).get(KEY, null);
		if (pref != null && pref.trim().length() > 0) {
			Reader input= new StringReader(pref);
			TemplateReaderWriter reader= new TemplateReaderWriter();
			TemplatePersistenceData[] datas;
			try {
				datas= reader.read(input);
				return datas.length > 0;
			} catch (IOException e) {
				// ignore
			}
		}
		return false;
	}


	public TemplatePersistenceData[] getTemplateData() {
		if (fProjectStore != null) {
			return fProjectStore.getTemplateData(true);
		} else {
			return fInstanceStore.getTemplateData(true);
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

			Set<String> datas= new HashSet<>();
			for (TemplatePersistenceData d : fProjectStore.getTemplateData(false)) {
				datas.add(d.getId());
			}

			for (TemplatePersistenceData orig : fInstanceStore.getTemplateData(false)) {
				if (!datas.contains(orig.getId())) {
					TemplatePersistenceData copy= new TemplatePersistenceData(new Template(orig.getTemplate()), orig.isEnabled(), orig.getId());
					fProjectStore.add(copy);
					copy.setDeleted(true);
				}
			}
		}
	}

	public boolean isProjectSpecific(String id) {
		if (id == null) {
			return false;
		}

		if (fProjectStore == null)
			return false;

		return fProjectStore.findTemplateById(id) != null;
	}


	public void setProjectSpecific(String id, boolean projectSpecific) {
		Assert.isNotNull(fProjectStore);

		TemplatePersistenceData data= fProjectStore.getTemplateData(id);
		if (data == null) {
			return; // does not exist
		} else {
			data.setDeleted(!projectSpecific);
		}
	}

	public void restoreDefaults() {
		if (fProjectStore == null) {
			fInstanceStore.restoreDefaults(false);
		} else {
			try {
				load();
			} catch (IOException e) {
				JavaManipulationPlugin.log(e);
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

	public void revertChanges() throws IOException {
		if (fProjectStore != null) {
			// nothing to do
		} else {
			fInstanceStore.load();
		}
	}
}
