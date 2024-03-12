/*******************************************************************************
 * Copyright (c) 2023 Red Hat Inc. and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.InlineMethodFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.QuickAssistProcessorUtil;

public class InlineDeprecatedMethodCleanUpCore extends AbstractMultiFix {

	public InlineDeprecatedMethodCleanUpCore() {
		this(Collections.emptyMap());
	}

	public InlineDeprecatedMethodCleanUpCore(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.REPLACE_DEPRECATED_CALLS);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.REPLACE_DEPRECATED_CALLS)) {
			return new String[] { MultiFixMessages.InlineDeprecatedMethodCleanUp_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		StringBuilder bld= new StringBuilder();
		bld.append("Class E {\n"); //$NON-NLS-1$
		bld.append("  /**\n"); //$NON-NLS-1$
		bld.append("   * @deprecated use {@link foo(int)} instead\n"); //$NON-NLS-1$
		bld.append("   * @param a - first int\n"); //$NON-NLS-1$
		bld.append("   * @param b - second int\n"); //$NON-NLS-1$
		bld.append("   */\n"); //$NON-NLS-1$
		bld.append("  @Deprecated\n"); //$NON-NLS-1$
		bld.append("  public static int foo(int a, int b) {\n"); //$NON-NLS-1$
		bld.append("    return foo(a + b);\n"); //$NON-NLS-1$
		bld.append("  }\n"); //$NON-NLS-1$
		bld.append("  public static int foo(int a) {\n"); //$NON-NLS-1$
		bld.append("    return a + 7;\n"); //$NON-NLS-1$
		bld.append("  }\n"); //$NON-NLS-1$
		bld.append("}\n\n"); //$NON-NLS-1$
		if (isEnabled(CleanUpConstants.REPLACE_DEPRECATED_CALLS)) {
			bld.append("return E.foo(x + y);\n"); //$NON-NLS-1$
		} else {
			bld.append("return E.foo(x, y);\n"); //$NON-NLS-1$
		}
		return bld.toString();
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit compilationUnit) throws CoreException {
		if (!isEnabled(CleanUpConstants.REPLACE_DEPRECATED_CALLS)) {
			return null;
		}
		if (compilationUnit == null)
			return null;

		final List<MethodInvocation> deprecatedInvocations= new ArrayList<>();
		ASTVisitor visitor= new ASTVisitor() {
			@Override
			public boolean visit(MethodInvocation node) {
				if (QuickAssistProcessorUtil.isDeprecatedMethodCallWithReplacement(node)) {
					deprecatedInvocations.add(node);
				}
				return true;
			}
		};
		compilationUnit.accept(visitor);
		if (deprecatedInvocations.isEmpty()) {
			return null;
		}
		return InlineMethodFixCore.create(getPreview(), compilationUnit, deprecatedInvocations);
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit, IProblemLocation[] problems) throws CoreException {
		return null;
	}

}
