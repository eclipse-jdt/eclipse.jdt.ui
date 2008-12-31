/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.persistence.TemplatePersistenceData;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.corext.template.java.CodeTemplateContextType;

import org.eclipse.jdt.internal.ui.viewsupport.ProjectTemplateStore;

public class TemplateStoreTest extends CoreTests {

	private static final Class THIS= TemplateStoreTest.class;

	private IJavaProject fJProject1;

	public TemplateStoreTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}


	public static Test suite() {
		return allTests();
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}


	protected void setUp() throws Exception {
	}


	protected void tearDown() throws Exception {
	}

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
		CodeTemplateContextType.ANNOTATIONBODY_ID,
		CodeTemplateContextType.FIELDCOMMENT_ID,
		CodeTemplateContextType.METHODCOMMENT_ID,
		CodeTemplateContextType.CONSTRUCTORCOMMENT_ID,
		CodeTemplateContextType.OVERRIDECOMMENT_ID,
		CodeTemplateContextType.DELEGATECOMMENT_ID,
		CodeTemplateContextType.GETTERCOMMENT_ID,
		CodeTemplateContextType.SETTERCOMMENT_ID,
	};

	private TemplatePersistenceData find(String id, TemplatePersistenceData[] templateData) {
		for (int i= 0; i < templateData.length; i++) {
			if (templateData[i].getId().equals(id)) {
				return templateData[i];
			}
		}
		return null;
	}

	public void testInstanceCodeTemplates() throws Exception {
		ProjectTemplateStore store= new ProjectTemplateStore(null);
		store.load();
		TemplatePersistenceData[] allTemplateDatas= store.getTemplateData();
		assertEquals(ALL_CODE_TEMPLATES.length, allTemplateDatas.length);
		for (int i= 0; i < ALL_CODE_TEMPLATES.length; i++) {
			// get it from the array
			TemplatePersistenceData fromArray=  find(ALL_CODE_TEMPLATES[i], allTemplateDatas);
			assertNotNull(fromArray);

			// get it from the store by id
			Template fromStore=  store.findTemplateById(ALL_CODE_TEMPLATES[i]);
			assertNotNull(fromStore);
			assertEquals(fromArray.getTemplate().getPattern(), fromStore.getPattern());
		}
		String newComment= "//Hello1";
		String templateId= CodeTemplateContextType.SETTERCOMMENT_ID;

		TemplatePersistenceData currData= find(templateId, allTemplateDatas);
		Template oldTemplate= currData.getTemplate();
		currData.setTemplate(new Template(oldTemplate.getName(), oldTemplate.getDescription(), oldTemplate.getContextTypeId(), newComment, oldTemplate.isAutoInsertable()));

		Template currTempl= store.findTemplateById(templateId);
		assertTrue(currTempl.getPattern().equals(newComment));

		store.restoreDefaults(); // restore
		currTempl= store.findTemplateById(templateId);
		assertFalse(currTempl.getPattern().equals(newComment));
	}


	public void testProjectCodeTemplates1() throws Exception {
		fJProject1= JavaProjectHelper.createJavaProject("TestProject1", "bin");
		try {
			ProjectTemplateStore projectStore= new ProjectTemplateStore(fJProject1.getProject());
			projectStore.load();
			ProjectTemplateStore instanceStore= new ProjectTemplateStore(null);
			instanceStore.load();


			TemplatePersistenceData[] allTemplateDatas= projectStore.getTemplateData();
			assertEquals(ALL_CODE_TEMPLATES.length, allTemplateDatas.length);
			for (int i= 0; i < ALL_CODE_TEMPLATES.length; i++) {
				// get it from the array
				TemplatePersistenceData fromArray=  find(ALL_CODE_TEMPLATES[i], allTemplateDatas);
				assertNotNull(fromArray);
				// get it from the store by id
				Template fromStore=  projectStore.findTemplateById(ALL_CODE_TEMPLATES[i]);
				assertNotNull(fromStore);
				assertEquals(fromArray.getTemplate().getPattern(), fromStore.getPattern());

				// equal to instance
				Template fromInstance=  instanceStore.findTemplateById(ALL_CODE_TEMPLATES[i]);
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
			assertTrue(currTempl.getPattern().equals(newComment));

			Template instanceElem= instanceStore.findTemplateById(templateId);
			assertFalse(instanceElem.getPattern().equals(currTempl.getPattern()));

			// remove project specific
			projectStore.setProjectSpecific(templateId, false);
			assertFalse(projectStore.isProjectSpecific(templateId));

			currTempl= projectStore.findTemplateById(templateId);
			assertFalse(currTempl.getPattern().equals(newComment));
			assertTrue(instanceElem.getPattern().equals(currTempl.getPattern()));

			// make project specific again and restore defaults
			projectStore.setProjectSpecific(templateId, true);
			assertTrue(projectStore.isProjectSpecific(templateId));

			projectStore.restoreDefaults(); // restore

			currTempl= projectStore.findTemplateById(templateId);
			assertFalse(currTempl.getPattern().equals(newComment));
			assertTrue(currTempl.getPattern().equals(instanceElem.getPattern()));
		} finally {
			JavaProjectHelper.delete(fJProject1);
		}
	}


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
			assertTrue(currTempl.getPattern().equals(newComment));

			// remove project specific
			projectStore.setProjectSpecific(templateId, false);
			projectStore.save();

			projectStore= new ProjectTemplateStore(fJProject1.getProject());
			projectStore.load();
			currTempl= projectStore.findTemplateById(templateId);
			assertFalse(currTempl.getPattern().equals(newComment));


		} finally {
			JavaProjectHelper.delete(fJProject1);
		}
	}


}
