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



public final class CompatibilityTemplateStore extends TemplateStore {


	private TemplateSet fLegacySet;

	public CompatibilityTemplateStore(ContextTypeRegistry registry, IPreferenceStore store, String key, TemplateSet legacySet) {
		super(registry, store, key);
		fLegacySet= legacySet;
	}

	public void load() throws IOException {
		super.load();
		
		if (fLegacySet != null) {
			
			List templates= new ArrayList(Arrays.asList(fLegacySet.getTemplates()));
			fLegacySet.clear();
			
			TemplatePersistenceData[] datas= getTemplateData(true);
			outer: for (int j= 0; j < datas.length; j++) {
				TemplatePersistenceData data= datas[j];
				Template orig= data.getTemplate();
				
				for (Iterator it= templates.listIterator(); it.hasNext();) {
					Template t= (Template) it.next();
					if (isSimilar(t, orig)) {
						if (!orig.getPattern().equals(t.getPattern()))
							add(new TemplatePersistenceData(t, true, data.getId()));
						it.remove(); // this one covered
						continue outer;
					}
				}
			}
			
			for (Iterator it= templates.iterator(); it.hasNext();) {
				add(new TemplatePersistenceData((Template) it.next(), true));
			}
			
			save();
//			fLegacySet= null;
		}
	}

	private boolean isSimilar(Template t, Template orig) {
		return orig.getName().equals(t.getName()) && orig.getContextTypeId().equals(t.getContextTypeId());
	}
}