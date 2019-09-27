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

import org.eclipse.jdt.internal.corext.template.java.CompilationUnitCompletion.Variable;


public class VarResolver extends AbstractVariableResolver {

	/**
	 * Default ctor for instantiation by the extension point.
	 */
	public VarResolver() {
		this("java.lang.Object"); //$NON-NLS-1$
	}

	VarResolver(String defaultType) {
		super(defaultType);
	}

	@Override
	protected Variable[] getVisibleVariables(String type, IJavaContext context) {
		Variable[] localVariables= context.getLocalVariables(type);
		Variable[] fields= context.getFields(type);

		Variable[] result= new Variable[localVariables.length + fields.length];

		System.arraycopy(localVariables, 0, result, 0, localVariables.length);
		System.arraycopy(fields, 0, result, localVariables.length, fields.length);

		return result;
	}
}
