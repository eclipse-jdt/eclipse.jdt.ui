/*******************************************************************************
 * Copyright (c) 2021 Fabrice TIERCELIN and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
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

import org.eclipse.jdt.internal.corext.fix.ArrayWithCurlyFixCore;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

public class ArrayWithCurlyCleanUpCore extends AbstractCleanUpCore {
	public ArrayWithCurlyCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public ArrayWithCurlyCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.ARRAY_WITH_CURLY);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.ARRAY_WITH_CURLY)) {
			return null;
		}

		return ArrayWithCurlyFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.ARRAY_WITH_CURLY)) {
			result.add(MultiFixMessages.ArrayWithCurlyCleanup_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.ARRAY_WITH_CURLY)) {
			return "double[] doubleArray = { 42.42 };\n"; //$NON-NLS-1$
		}

		return "double[] doubleArray = new double[] { 42.42 };\n"; //$NON-NLS-1$
	}
}
