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
 * Resolves template variables to a local variable that is assignment-compatible with the variable
 * instance' class parameter.
 *
 * @since 3.3
 */
public class LocalVarResolver extends AbstractVariableResolver {

	/**
	 * Default ctor for instantiation by the extension point.
	 */
	public LocalVarResolver() {
		this("java.lang.Object"); //$NON-NLS-1$
	}

	LocalVarResolver(String defaultType) {
		super(defaultType);
	}

	@Override
	protected Variable[] getVisibleVariables(String type, IJavaContext context) {
		return context.getLocalVariables(type);
	}

}
