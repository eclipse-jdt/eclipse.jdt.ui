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
import org.eclipse.jdt.internal.corext.fix.StringBufferToStringBuilderFixCore;

public class StringBufferToStringBuilderCleanUpCore extends AbstractCleanUpCore {

	public StringBufferToStringBuilderCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	public StringBufferToStringBuilderCleanUpCore() {
	}

	@Override
	public CleanUpRequirementsCore getRequirementsCore() {
		return new CleanUpRequirementsCore(requireAST(), false, false, null);
	}

	public boolean requireAST() {
		return isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER);
	}

	@Override
	public ICleanUpFixCore createFixCore(final CleanUpContextCore context) throws CoreException {
		CompilationUnit compilationUnit= context.getAST();

		if (compilationUnit == null || !isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER)) {
			return null;
		}

		return StringBufferToStringBuilderFixCore.createCleanUp(compilationUnit, isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS));
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER)) {
			if (isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS)) {
				return new String[] {MultiFixMessages.StringBuilderForLocalVarsOnlyCleanUp_description};
			} else {
				return new String[] {MultiFixMessages.StringBufferToStringBuilderCleanUp_description};
			}
		}
		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();

		if (isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER) && !isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER_FOR_LOCALS)) {
			bld.append("public void foo(StringBuilder x) {\n"); //$NON-NLS-1$
		} else {
			bld.append("public void foo(StringBuffer x) {\n"); //$NON-NLS-1$
		}
		if (isEnabled(CleanUpConstants.STRINGBUFFER_TO_STRINGBUILDER)) {
			bld.append("    StringBuilder y = new StringBuilder();\n"); //$NON-NLS-1$
		} else {
			bld.append("    StringBuffer y = new StringBuffer();\n"); //$NON-NLS-1$
		}
		bld.append("    y.append(\"a string\");\n"); //$NON-NLS-1$
		bld.append("    System.out.println(y.toString());\n"); //$NON-NLS-1$
		bld.append("}\n"); //$NON-NLS-1$

		return bld.toString();
	}
}
