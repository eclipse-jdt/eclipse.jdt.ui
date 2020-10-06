/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - created from VariableDeclarationCleanUp
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.VariableDeclarationFixCore;

public class VariableDeclarationCleanUpCore extends AbstractCleanUpCore {

	public VariableDeclarationCleanUpCore(Map<String, String> options) {
		super(options);
	}

	public VariableDeclarationCleanUpCore() {
		super();
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	private boolean requireAST() {
		boolean addFinal= isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		if (!addFinal)
			return false;

		return isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS) ||
				isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS) ||
				isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES);
	}

	@Override
	public ICleanUpFixCore createFixCore(CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();
		if (compilationUnit == null)
			return null;

		boolean addFinal= isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL);
		if (!addFinal)
			return null;

		return VariableDeclarationFixCore.createCleanUp(compilationUnit,
				isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS),
				isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS),
				isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES));
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL) && isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS))
			result.add(MultiFixMessages.VariableDeclarationCleanUp_AddFinalField_description);
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL) && isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS))
			result.add(MultiFixMessages.VariableDeclarationCleanUp_AddFinalParameters_description);
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL) && isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES))
			result.add(MultiFixMessages.VariableDeclarationCleanUp_AddFinalLocals_description);

		return result.toArray(new String[result.size()]);
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL) && isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PRIVATE_FIELDS)) {
			bld.append("private final int i= 0;\n"); //$NON-NLS-1$
		} else {
			bld.append("private int i= 0;\n"); //$NON-NLS-1$
		}
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL) && isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_PARAMETERS)) {
			bld.append("public void foo(final int j) {\n"); //$NON-NLS-1$
		} else {
			bld.append("public void foo(int j) {\n"); //$NON-NLS-1$
		}
		if (isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL) && isEnabled(CleanUpConstants.VARIABLE_DECLARATIONS_USE_FINAL_LOCAL_VARIABLES)) {
			bld.append("    final int k;\n"); //$NON-NLS-1$
			bld.append("    int h;\n"); //$NON-NLS-1$
			bld.append("    h= 0;\n"); //$NON-NLS-1$
			bld.append("}\n"); //$NON-NLS-1$
		} else {
			bld.append("    int k, h;\n"); //$NON-NLS-1$
			bld.append("    h= 0;\n"); //$NON-NLS-1$
			bld.append("}\n\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}

}
