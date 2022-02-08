/*******************************************************************************
 * Copyright (c) 2022 Red Hat and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.text.java.IProblemLocation;

import org.eclipse.jdt.internal.ui.text.correction.ProblemLocation;

/**
 * An extension of {@link AbstractCleanUpCoreWrapper} for cases where the wrapped
 * cleanup extends {@link AbstractMultiFixCore}
 *
 * @param <T> The type of the cleanup this class forwards to.
 */
public class AbstractMultiFixCoreWrapper<T extends AbstractMultiFixCore> extends AbstractCleanUpCoreWrapper<T> implements IMultiFix {

	protected AbstractMultiFixCoreWrapper(Map<String, String> settings, T wrapped) {
		super(settings, wrapped);
	}

	@Override
	public int computeNumberOfFixes(CompilationUnit compilationUnit) {
		return cleanUpCore.computeNumberOfFixes(compilationUnit);
	}

	@Override
	public boolean canFix(ICompilationUnit compilationUnit, IProblemLocation problem) {
		return cleanUpCore.canFix(compilationUnit, (ProblemLocation)problem);
	}
}
