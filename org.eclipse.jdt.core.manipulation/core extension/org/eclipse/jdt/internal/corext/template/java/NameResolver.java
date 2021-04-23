/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
 *     Sebastian Davids: sdavids@gmx.de - see bug 25376
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.internal.ui.text.template.contentassist.MultiVariable;

/**
 * Resolves template variables to non-conflicting names that adhere to the naming conventions and
 * match the parameter (fully qualified name).
 *
 * @since 3.3
 */
public class NameResolver extends TemplateVariableResolver {

	private final String fDefaultType;

	/**
	 * Default ctor for instantiation by the extension point.
	 */
	public NameResolver() {
		this("java.lang.Object"); //$NON-NLS-1$
	}

	NameResolver(String defaultType) {
		fDefaultType= defaultType;
	}

	/*
	 * @see org.eclipse.jface.text.templates.TemplateVariableResolver#resolve(org.eclipse.jface.text.templates.TemplateVariable, org.eclipse.jface.text.templates.TemplateContext)
	 */
	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		List<String> params= variable.getVariableType().getParams();
		String param;
		if (params.size() == 0)
			param= fDefaultType;
		else
			param= params.get(0);
		IJavaContext jc= (IJavaContext) context;
		TemplateVariable ref= jc.getTemplateVariable(param);
		MultiVariable mv= (MultiVariable) variable;
		if (ref instanceof MultiVariable) {
			// reference is another variable
			MultiVariable refVar= (MultiVariable) ref;
			jc.addDependency(refVar, mv);

			Object[] types= flatten(refVar.getAllChoices());
			for (Object type : types) {
				String[] names= jc.suggestVariableNames(mv.toString(type));
				mv.setChoices(type, names);
			}

			mv.setKey(refVar.getCurrentChoice());
			jc.markAsUsed(mv.getDefaultValue());
		} else {
			// reference is a Java type name
			jc.addImport(param);
			String[] names= jc.suggestVariableNames(param);
			mv.setChoices(names);
			jc.markAsUsed(names[0]);
		}
	}

	private Object[] flatten(Object[][] allValues) {
		List<Object> flattened= new ArrayList<>(allValues.length);
		for (Object[] allValue : allValues) {
			flattened.addAll(Arrays.asList(allValue));
		}
		return flattened.toArray(new Object[flattened.size()]);
	}
}
