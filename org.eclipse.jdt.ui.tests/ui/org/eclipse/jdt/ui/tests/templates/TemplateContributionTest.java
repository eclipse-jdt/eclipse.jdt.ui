/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.templates;

import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateTranslator;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.jdt.internal.corext.template.java.JavaContextType;
import org.eclipse.jdt.internal.corext.template.java.JavaDocContextType;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class TemplateContributionTest extends TestCase {

	public static Test suite() {
		return new TestSuite(TemplateContributionTest.class);
	}

	public void testJavaContribution() throws Exception {
		ContextTypeRegistry registry= JavaPlugin.getDefault().getTemplateContextRegistry();
		TemplateContextType javaContext= registry.getContextType(JavaContextType.NAME);
	
		TemplateStore templateStore= JavaPlugin.getDefault().getTemplateStore();
		Template[] javaTemplates= templateStore.getTemplates(JavaContextType.NAME);
		
		for (int i= 0; i < javaTemplates.length; i++) {
			Template template= javaTemplates[i];
			TemplateTranslator translator= new TemplateTranslator();
			TemplateBuffer buffer= translator.translate(template);
			TemplateVariable[] variables= buffer.getVariables();
			for (int j= 0; j < variables.length; j++) {
				TemplateVariable variable= variables[j];
				if (!variable.getType().equals(variable.getName())) {
					assertTrue("No resolver found for variable '" + variable.getType() + "' in template '" + template.getName() + "'\n\n" + template.getPattern(), canHandle(javaContext, variable));
				}
			}
		}
	}
	
	public void testJavaDocContribution() throws Exception {
		ContextTypeRegistry registry= JavaPlugin.getDefault().getTemplateContextRegistry();
		TemplateContextType javaContext= registry.getContextType(JavaDocContextType.NAME);
	
		TemplateStore templateStore= JavaPlugin.getDefault().getTemplateStore();
		Template[] javaTemplates= templateStore.getTemplates(JavaDocContextType.NAME);
		
		for (int i= 0; i < javaTemplates.length; i++) {
			Template template= javaTemplates[i];
			TemplateTranslator translator= new TemplateTranslator();
			TemplateBuffer buffer= translator.translate(template);
			TemplateVariable[] variables= buffer.getVariables();
			for (int j= 0; j < variables.length; j++) {
				TemplateVariable variable= variables[j];
				if (!variable.getType().equals(variable.getName())) {
					assertTrue("No resolver found for variable '" + variable.getType() + "' in template'" + template.getName() + "'\n\n" + template.getPattern(), canHandle(javaContext, variable));
				}
			}
		}
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
