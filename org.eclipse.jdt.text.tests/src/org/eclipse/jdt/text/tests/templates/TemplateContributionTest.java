/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.templates;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaDocContextType;
import org.eclipse.jdt.internal.corext.template.java.SWTContextType;

import org.eclipse.jdt.internal.ui.JavaPlugin;


/**
 * Template contribution tests.
 *
 * @since 3.4
 */
public class TemplateContributionTest extends TestCase {

	public static Test suite() {
		return new TestSuite(TemplateContributionTest.class);
	}

	private void checkContribution(String resolverContextTypeId, String contextTypeId) throws TemplateException {
		ContextTypeRegistry registry= JavaPlugin.getDefault().getTemplateContextRegistry();
		TemplateContextType context= registry.getContextType(resolverContextTypeId);

		TemplateStore templateStore= JavaPlugin.getDefault().getTemplateStore();
		Template[] templates= templateStore.getTemplates(contextTypeId);

		for (int i= 0; i < templates.length; i++) {
			Template template= templates[i];
			TemplateTranslator translator= new TemplateTranslator();
			TemplateBuffer buffer= translator.translate(template);
			TemplateVariable[] variables= buffer.getVariables();
			for (int j= 0; j < variables.length; j++) {
				TemplateVariable variable= variables[j];
				if (!variable.getType().equals(variable.getName())) {
					assertTrue("No resolver found for variable '" + variable.getType() + "' in template '" + template.getName() + "'\n\n" + template.getPattern(), canHandle(context, variable));
				}
			}
		}
	}

	public void testJavaContribution() throws Exception {
		checkContribution(JavaContextType.ID_ALL, JavaContextType.ID_ALL);
		checkContribution(JavaContextType.ID_ALL, JavaContextType.ID_MEMBERS);
		checkContribution(JavaContextType.ID_ALL, JavaContextType.ID_STATEMENTS);
		checkContribution(JavaContextType.ID_MEMBERS, JavaContextType.ID_MEMBERS);
		checkContribution(JavaContextType.ID_STATEMENTS, JavaContextType.ID_STATEMENTS);
	}

	public void testJavaDocContribution() throws Exception {
		checkContribution(JavaDocContextType.ID, JavaDocContextType.ID);
	}

	public void testSWTContributionAll() throws Exception {
		checkContribution(SWTContextType.ID_ALL, SWTContextType.ID_ALL);
		checkContribution(SWTContextType.ID_ALL, SWTContextType.ID_MEMBERS);
		checkContribution(SWTContextType.ID_ALL, SWTContextType.ID_STATEMENTS);
		checkContribution(SWTContextType.ID_MEMBERS, SWTContextType.ID_MEMBERS);
		checkContribution(SWTContextType.ID_STATEMENTS, SWTContextType.ID_STATEMENTS);
	}

	private boolean canHandle(TemplateContextType context, TemplateVariable variable) {
		for (Iterator iterator= context.resolvers(); iterator.hasNext();) {
			TemplateVariableResolver resolver= (TemplateVariableResolver) iterator.next();
			if (variable.getType().equals(resolver.getType()))
				return true;
		}
		return false;
	}

}
