/*******************************************************************************
 * Copyright (c) 2020 Fabrice TIERCELIN and others.
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

import java.util.Collections;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.TryWithResourceFixCore;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;
import org.eclipse.jdt.ui.text.java.IProblemLocation;

/**
 * A fix that changes code to make use of Java 7 try-with-resources feature. In particular, it removes now useless finally clauses.
 */
public class TryWithResourceCleanUp extends AbstractMultiFix implements ICleanUpFix {
	public TryWithResourceCleanUp() {
		this(Collections.emptyMap());
	}

	public TryWithResourceCleanUp(final Map<String, String> options) {
		super(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		boolean requireAST= isEnabled(CleanUpConstants.TRY_WITH_RESOURCE);
		return new CleanUpRequirements(requireAST, false, false, null);
	}

	@Override
	public String[] getStepDescriptions() {
		if (isEnabled(CleanUpConstants.TRY_WITH_RESOURCE)) {
			return new String[] { MultiFixMessages.TryWithResourceCleanup_description };
		}

		return new String[0];
	}

	@Override
	public String getPreview() {
		if (isEnabled(CleanUpConstants.TRY_WITH_RESOURCE)) {
			return """
				final FileInputStream inputStream = new FileInputStream("out.txt");
				try (inputStream) {
				    System.out.println(inputStream.read());
				}
				
				
				"""; //$NON-NLS-1$
		}

		return """
			final FileInputStream inputStream = new FileInputStream("out.txt");
			try {
			    System.out.println(inputStream.read());
			} finally {
			    inputStream.close();
			}
			"""; //$NON-NLS-1$
	}

	@Override
	protected ICleanUpFix createFix(final CompilationUnit unit) throws CoreException {
		if (!isEnabled(CleanUpConstants.TRY_WITH_RESOURCE) || !JavaModelUtil.is1d7OrHigher(unit.getJavaElement().getJavaProject())) {
			return null;
		}

		ICleanUpFix cleanUpFixCore= TryWithResourceFixCore.createCleanUp(unit);
		return cleanUpFixCore;
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
