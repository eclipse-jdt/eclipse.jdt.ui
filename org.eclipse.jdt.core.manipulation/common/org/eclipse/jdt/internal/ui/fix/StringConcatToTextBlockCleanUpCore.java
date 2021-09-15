/*******************************************************************************
 * Copyright (c) 2021 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CleanUpContextCore;
import org.eclipse.jdt.core.manipulation.CleanUpRequirementsCore;
import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.StringConcatToTextBlockFixCore;

public class StringConcatToTextBlockCleanUpCore extends AbstractCleanUpCore {

	public StringConcatToTextBlockCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public StringConcatToTextBlockCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK)) {
			return null;
		}

		return StringConcatToTextBlockFixCore.createCleanUp(compilationUnit);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK)) {
			return new String[] {MultiFixMessages.StringConcatToTextBlockCleanUp_description};
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.STRINGCONCAT_TO_TEXTBLOCK)) {
			bld.append("String buf = \"\"\"\n"); //$NON-NLS-1$
			bld.append("    public class A {\n"); //$NON-NLS-1$
			bld.append("        public void foo() {\n"); //$NON-NLS-1$
			bld.append("        }\n"); //$NON-NLS-1$
			bld.append("    }\n"); //$NON-NLS-1$
			bld.append("    \"\"\";\n"); //$NON-NLS-1$
		} else {
			bld.append("String buf = \"\" +\n"); //$NON-NLS-1$
			bld.append("    \"public class A {\" +\n"); //$NON-NLS-1$
			bld.append("    \"    public void foo() {\" +\n"); //$NON-NLS-1$
			bld.append("    \"    }\" + \n"); //$NON-NLS-1$
			bld.append("    \"}\";\n"); //$NON-NLS-1$
		}

		return bld.toString();
	}
}
