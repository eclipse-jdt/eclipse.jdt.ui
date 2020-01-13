/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

/**
 * Resolver for the <code>import</code> variable. Resolves to a
 * set of import statements.
 *
 * @since 3.4
 */
public class ImportsResolver extends TemplateVariableResolver {

	public ImportsResolver(String type, String description) {
		super(type, description);
	}

	/**
	 * Default ctor for instantiation by the extension point.
	 */
	public ImportsResolver() {
	}

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		variable.setUnambiguous(true);
		variable.setValue(""); //$NON-NLS-1$

		if (context instanceof JavaContext) {
			JavaContext jc= (JavaContext) context;
			for (String typeName : variable.getVariableType().getParams()) {
				jc.addImport(typeName);
			}
		} else {
			super.resolve(variable, context);
		}
	}

	@Override
	protected String[] resolveAll(TemplateContext context) {
		return new String[0];
	}
}
