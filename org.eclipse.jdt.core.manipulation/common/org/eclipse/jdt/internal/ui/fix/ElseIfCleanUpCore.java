/*******************************************************************************
 * Copyright (c) 2020, 2024 Fabrice TIERCELIN and others.
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
 *     Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.ElseIfFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that uses the <code>else if</code> pseudo keyword.
 */
public class ElseIfCleanUpCore extends AbstractMultiFix implements ICleanUpFix {
	public ElseIfCleanUpCore() {
		this(Collections.emptyMap());
	}

	public ElseIfCleanUpCore(Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.ELSE_IF);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.ELSE_IF)) {
			return new String[] { MultiFixMessages.CodeStyleCleanUp_ElseIf_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.ELSE_IF)) {
			return "" //$NON-NLS-1$
					+ "if (isValid) {\n" //$NON-NLS-1$
					+ "  System.out.println(isValid);\n" //$NON-NLS-1$
					+ "} else if (isEnabled) {\n" //$NON-NLS-1$
					+ "  System.out.println(isEnabled);\n" //$NON-NLS-1$
					+ "}\n\n\n"; //$NON-NLS-1$
		}

		return "" //$NON-NLS-1$
				+ "if (isValid) {\n" //$NON-NLS-1$
				+ "  System.out.println(isValid);\n" //$NON-NLS-1$
				+ "} else {\n" //$NON-NLS-1$
				+ "  if (isEnabled) {\n" //$NON-NLS-1$
				+ "    System.out.println(isEnabled);\n" //$NON-NLS-1$
				+ "  }\n" //$NON-NLS-1$
				+ "}\n"; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.ELSE_IF)) {
			return null;
		}

		return ElseIfFixCore.createCleanUp(unit);
	}

	@Override
	public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
		return null;
	}

	@Override
	public boolean canFix(final ICompilationUnit compilationUnit, final IProblemLocation problem) {
		return false;
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit, final IProblemLocation[] problems) throws CoreException {
		return null;
	}

}
