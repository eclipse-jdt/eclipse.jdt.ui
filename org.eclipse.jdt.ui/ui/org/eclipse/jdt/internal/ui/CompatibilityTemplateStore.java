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
package org.eclipse.jdt.internal.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;
import org.eclipse.jface.text.templates.persistence.TemplateSet;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplates;


/**
 * @deprecated don't use
 */
public final class CompatibilityTemplateStore extends TemplateStore {


	private TemplateSet fLegacySet;

	public CompatibilityTemplateStore(ContextTypeRegistry registry, IPreferenceStore store, String key, TemplateSet legacySet) {
		super(registry, store, key);
		fLegacySet= legacySet;
	}

	public void load() throws IOException {
		super.load();
		
		if (fLegacySet != null) {
			
			List legacyTemplates= new ArrayList(Arrays.asList(fLegacySet.getTemplates()));
			fLegacySet.clear();
			
			TemplatePersistenceData[] datas= getTemplateData(true);
			for (Iterator it= legacyTemplates.listIterator(); it.hasNext();) {
				Template t= (Template) it.next();
				TemplatePersistenceData orig= findSimilarTemplate(datas, t);
				if (orig == null) { // no contributed match for the old template found
					if (!isCodeTemplates())
						add(new TemplatePersistenceData(t, true));
				} else { // a contributed template seems to be the descendant of the non-id template t
					if (!orig.getTemplate().getPattern().equals(t.getPattern()))
						// add as modified contributed template if changed compared to the original
						orig.setTemplate(t);
				}
			}
			
			save();
//			fLegacySet= null;
		}
	}
	
	private TemplatePersistenceData findSimilarTemplate(TemplatePersistenceData[] datas, Template template) {
		 for (int i= 0; i < datas.length; i++) {
			TemplatePersistenceData data= datas[i];
			Template orig= data.getTemplate();
			if (isSimilar(template, orig))
				return data;
		 }
		 
		 return null;
	}

	private boolean isSimilar(Template t, Template orig) {
		return orig.getName().equals(t.getName()) && orig.getContextTypeId().equals(t.getContextTypeId())
				&& (isCodeTemplates() || orig.getDescription().equals(t.getDescription())); // only use description for templates (for, while...)
	}
	
	private boolean isCodeTemplates() {
		return fLegacySet instanceof CodeTemplates;
	}
}