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

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ValueOfRatherThanInstantiationFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class ValueOfRatherThanInstantiationCleanUpCore extends AbstractCleanUp {
	public ValueOfRatherThanInstantiationCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public ValueOfRatherThanInstantiationCleanUpCore() {
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION);
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION)) {
			return null;
		}

		return ValueOfRatherThanInstantiationFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION)) {
			result.add(MultiFixMessages.ValueOfRatherThanInstantiationCleanup_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.VALUEOF_RATHER_THAN_INSTANTIATION)) {
			return """
				Object characterObject = Character.valueOf('a');
				Byte.valueOf("0").byteValue();
				long l = 42;
				"""; //$NON-NLS-1$
		}

		return """
			Object characterObject = Character.valueOf('a');
			new Byte("0").byteValue();
			long l = new Long(42);
			"""; //$NON-NLS-1$
	}
}
