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
 *  Variable resolver for variable <code>importStatic</code>. Resolves to
 *  static import statements.
 *
 *  @since 3.4
 */
public class StaticImportResolver extends TemplateVariableResolver {

	public StaticImportResolver(String type, String description) {
		super(type, description);
	}

	/**
	 * Default ctor for instantiation by the extension point.
	 */
	public StaticImportResolver() {
	}

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		variable.setUnambiguous(true);
		variable.setValue(""); //$NON-NLS-1$

		if (context instanceof JavaContext) {
			JavaContext jc= (JavaContext) context;
			for (String qualifiedMemberName : variable.getVariableType().getParams()) {
				jc.addStaticImport(qualifiedMemberName);
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
