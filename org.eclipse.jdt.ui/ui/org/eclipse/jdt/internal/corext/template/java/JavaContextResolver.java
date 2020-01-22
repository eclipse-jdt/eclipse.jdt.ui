/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariable;
import org.eclipse.jface.text.templates.TemplateVariableResolver;

import org.eclipse.jdt.core.IJavaElement;

/**
 * Resolve variables from the context of a Java compilation unit.
 */
public class JavaContextResolver extends TemplateVariableResolver {

	private static String PACKAGE_DECLARATION = "package_declaration"; //$NON-NLS-1$
	private static String CLASS_NAME = "class_name"; //$NON-NLS-1$

	public JavaContextResolver() {
	}

	public JavaContextResolver(String type, String description) {
		super(type, description);
	}

	@Override
	public void resolve(TemplateVariable variable, TemplateContext context) {
		if (context instanceof JavaContext) {
			JavaContext jc = (JavaContext) context;
			if (PACKAGE_DECLARATION.equals(variable.getName())) {
				IJavaElement pkgCandidate = jc.getCompilationUnit().getParent();
				String pkgNS = null;
				if (pkgCandidate != null &&
						pkgCandidate.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					pkgNS = pkgCandidate.getElementName();
				} else {
					pkgNS = jc.getCompilationUnit().getPath().toOSString()
							.replaceAll("/", ".") //$NON-NLS-1$ //$NON-NLS-2$
							.replaceAll("." + jc.getCompilationUnit().getElementName(), "") //$NON-NLS-1$ //$NON-NLS-2$
							.substring(1);
				}
				variable.setValue("package " + pkgNS + ";"); //$NON-NLS-1$ //$NON-NLS-2$
			} else if (CLASS_NAME.equals(variable.getName())) {
				String cName = jc.getCompilationUnit().getElementName().replaceAll(".java", ""); //$NON-NLS-1$ //$NON-NLS-2$
				variable.setValue(cName);
			}
		}
	}

}
