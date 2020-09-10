/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.text.templates.TemplatePersistenceData;

import org.eclipse.jface.text.templates.Template;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.ProjectTemplateStore;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

public class TemplateStoreTest extends CoreTests {

	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private IJavaProject fJProject1;

	private static final String[] ALL_CODE_TEMPLATES= new String[] {
		CodeTemplateContextType.CATCHBLOCK_ID,
		CodeTemplateContextType.METHODSTUB_ID,
		CodeTemplateContextType.NEWTYPE_ID,
		CodeTemplateContextType.CONSTRUCTORSTUB_ID,
		CodeTemplateContextType.GETTERSTUB_ID,
		CodeTemplateContextType.SETTERSTUB_ID,
		CodeTemplateContextType.FILECOMMENT_ID,
		CodeTemplateContextType.TYPECOMMENT_ID,
		CodeTemplateContextType.CLASSBODY_ID,
		CodeTemplateContextType.INTERFACEBODY_ID,
		CodeTemplateContextType.ENUMBODY_ID,
		CodeTemplateContextType.RECORDBODY_ID,
		CodeTemplateContextType.ANNOTATIONBODY_ID,
		CodeTemplateContextType.FIELDCOMMENT_ID,
		CodeTemplateContextType.METHODCOMMENT_ID,
		CodeTemplateContextType.CONSTRUCTORCOMMENT_ID,
		CodeTemplateContextType.OVERRIDECOMMENT_ID,
		CodeTemplateContextType.DELEGATECOMMENT_ID,
		CodeTemplateContextType.GETTERCOMMENT_ID,
		CodeTemplateContextType.SETTERCOMMENT_ID,
		CodeTemplateContextType.MODULECOMMENT_ID,
	};

	private TemplatePersistenceData find(String id, TemplatePersistenceData[] templateData) {
		for (TemplatePersistenceData t : templateData) {
			if (t.getId().equals(id)) {
				return t;
			}
		}
		return null;
	}

	@Test
	public void testInstanceCodeTemplates() throws Exception {
		ProjectTemplateStore store= new ProjectTemplateStore(null);
		store.load();
		TemplatePersistenceData[] allTemplateDatas= store.getTemplateData();
		assertEquals(ALL_CODE_TEMPLATES.length, allTemplateDatas.length);
		for (String t : ALL_CODE_TEMPLATES) {
			// get it from the array
			TemplatePersistenceData fromArray= find(t, allTemplateDatas);
			assertNotNull(fromArray);
			// get it from the store by id
			Template fromStore= store.findTemplateById(t);
			assertNotNull(fromStore);
			assertEquals(fromArray.getTemplate().getPattern(), fromStore.getPattern());
		}
		String newComment= "//Hello1";
		String templateId= CodeTemplateContextType.SETTERCOMMENT_ID;

		TemplatePersistenceData currData= find(templateId, allTemplateDatas);
		Template oldTemplate= currData.getTemplate();
		currData.setTemplate(new Template(oldTemplate.getName(), oldTemplate.getDescription(), oldTemplate.getContextTypeId(), newComment, oldTemplate.isAutoInsertable()));

		Template currTempl= store.findTemplateById(templateId);
		assertEquals(currTempl.getPattern(), newComment);

		store.restoreDefaults(); // restore
		currTempl= store.findTemplateById(templateId);
		assertNotEquals(currTempl.getPattern(), newComment);
	}


	@Test
	public void testProjectCodeTemplates1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		try {
			ProjectTemplateStore projectStore= new ProjectTemplateStore(fJProject1.getProject());
			projectStore.load();
			ProjectTemplateStore instanceStore= new ProjectTemplateStore(null);
			instanceStore.load();


			TemplatePersistenceData[] allTemplateDatas= projectStore.getTemplateData();
			assertEquals(ALL_CODE_TEMPLATES.length, allTemplateDatas.length);
			for (String t : ALL_CODE_TEMPLATES) {
				// get it from the array
				TemplatePersistenceData fromArray= find(t, allTemplateDatas);
				assertNotNull(fromArray);
				// get it from the store by id
				Template fromStore= projectStore.findTemplateById(t);
				assertNotNull(fromStore);
				assertEquals(fromArray.getTemplate().getPattern(), fromStore.getPattern());
				// equal to instance
				Template fromInstance= instanceStore.findTemplateById(t);
				assertNotNull(fromInstance);
				assertEquals(fromInstance.getPattern(), fromStore.getPattern());
			}

			String newComment= "//Hello2";
			String templateId= CodeTemplateContextType.SETTERCOMMENT_ID;

			// make project specific
			projectStore.setProjectSpecific(templateId, true);
			assertTrue(projectStore.isProjectSpecific(templateId));


			// modify template
			TemplatePersistenceData currData= find(templateId, allTemplateDatas);
			Template oldTemplate= currData.getTemplate();
			currData.setTemplate(new Template(oldTemplate.getName(), oldTemplate.getDescription(), oldTemplate.getContextTypeId(), newComment, oldTemplate.isAutoInsertable()));

			Template currTempl= projectStore.findTemplateById(templateId);
			assertEquals(currTempl.getPattern(), newComment);

			Template instanceElem= instanceStore.findTemplateById(templateId);
			assertNotEquals(instanceElem.getPattern(), currTempl.getPattern());

			// remove project specific
			projectStore.setProjectSpecific(templateId, false);
			assertFalse(projectStore.isProjectSpecific(templateId));

			currTempl= projectStore.findTemplateById(templateId);
			assertNotEquals(currTempl.getPattern(), newComment);
			assertEquals(instanceElem.getPattern(), currTempl.getPattern());

			// make project specific again and restore defaults
			projectStore.setProjectSpecific(templateId, true);
			assertTrue(projectStore.isProjectSpecific(templateId));

			projectStore.restoreDefaults(); // restore

			currTempl= projectStore.findTemplateById(templateId);
			assertNotEquals(currTempl.getPattern(), newComment);
			assertEquals(currTempl.getPattern(), instanceElem.getPattern());
		} finally {
			JavaProjectHelper.delete(fJProject1);
		}
	}


	@Test
	public void testProjectCodeTemplates2() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		try {
			ProjectTemplateStore projectStore= new ProjectTemplateStore(fJProject1.getProject());
			projectStore.load();

			String newComment= "//Hello3";
			String templateId= CodeTemplateContextType.SETTERCOMMENT_ID;

			// make project specific
			projectStore.setProjectSpecific(templateId, true);

			// modify template
			TemplatePersistenceData currData= find(templateId, projectStore.getTemplateData());
			Template oldTemplate= currData.getTemplate();
			currData.setTemplate(new Template(oldTemplate.getName(), oldTemplate.getDescription(), oldTemplate.getContextTypeId(), newComment, oldTemplate.isAutoInsertable()));

			projectStore.save();

			// load store again
			projectStore= new ProjectTemplateStore(fJProject1.getProject());
			projectStore.load();

			Template currTempl= projectStore.findTemplateById(templateId);
			assertEquals(currTempl.getPattern(), newComment);

			// remove project specific
			projectStore.setProjectSpecific(templateId, false);
			projectStore.save();

			projectStore= new ProjectTemplateStore(fJProject1.getProject());
			projectStore.load();
			currTempl= projectStore.findTemplateById(templateId);
			assertNotEquals(currTempl.getPattern(), newComment);


		} finally {
			JavaProjectHelper.delete(fJProject1);
		}
	}

}
