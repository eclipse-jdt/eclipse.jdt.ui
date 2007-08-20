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
package org.eclipse.jdt.internal.corext.template.java;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

public class ImportsResolver extends TemplateVariableResolver {

	public ImportsResolver(String type, String description) {
		super(type, description);
	}

	public ImportsResolver() {
	}

	/**
	 * {@inheritDoc}
	 */
	public void resolve(TemplateVariable variable, TemplateContext context) {
		variable.setUnambiguous(true);

		if (context instanceof JavaContext) {
			JavaContext jc= (JavaContext) context;
			List params= variable.getVariableType().getParams();
			if (params.size() > 0) {
				for (Iterator iterator= params.iterator(); iterator.hasNext();) {
					String typeName= (String) iterator.next();
					jc.addImport(typeName);
				}
			}
		} else {
			super.resolve(variable, context);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	protected String[] resolveAll(TemplateContext context) {
		return new String[0];
	}
}
