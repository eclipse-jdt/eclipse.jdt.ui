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
 *     Microsoft Corporation - moved template related code to jdt.core.manipulation - https://bugs.eclipse.org/549989
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.template.java;

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.Variable;

/**
 * Resolves a template variable to a field that is assignment-compatible
 * with the variable instance' class parameter.
 *
 * @since 3.3
 */
public class FieldResolver extends AbstractVariableResolver {

	/**
	 * Default constructor for instantiation by the extension point.
	 */
	public FieldResolver() {
		this("java.lang.Object"); //$NON-NLS-1$
	}

	FieldResolver(String defaultType) {
		super(defaultType);
	}

	@Override
	protected Variable[] getVisibleVariables(String type, IJavaContext context) {
		return context.getFields(type);
	}

}
