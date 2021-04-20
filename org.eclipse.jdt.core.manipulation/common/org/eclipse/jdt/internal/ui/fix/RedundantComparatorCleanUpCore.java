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

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.RedundantComparatorFixCore;

public class RedundantComparatorCleanUpCore extends AbstractCleanUpCore {
	public RedundantComparatorCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public RedundantComparatorCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.REDUNDANT_COMPARATOR);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.REDUNDANT_COMPARATOR)) {
			return null;
		}

		return RedundantComparatorFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		List<String> result= new ArrayList<>();

		if (isEnabled(CleanUpConstants.REDUNDANT_COMPARATOR)) {
			result.add(MultiFixMessages.RedundantComparatorCleanUp_description);
		}

		return result.toArray(new String[0]);
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.REDUNDANT_COMPARATOR)) {
			return "Collections.sort(listToSort);\n\n\n\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "Collections.sort(listToSort, new Comparator<Date>() {\n" //$NON-NLS-1$
				+ "    @Override\n" //$NON-NLS-1$
				+ "    public int compare(Date o1, Date o2) {\n" //$NON-NLS-1$
				+ "        return o1.compareTo(o2);\n" //$NON-NLS-1$
				+ "    }\n" //$NON-NLS-1$
				+ "});\n"; //$NON-NLS-1$
	}
}
