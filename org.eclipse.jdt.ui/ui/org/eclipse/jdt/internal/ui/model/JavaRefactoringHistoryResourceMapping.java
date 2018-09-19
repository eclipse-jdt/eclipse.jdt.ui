/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.model;

import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.core.refactoring.model.AbstractRefactoringHistoryResourceMapping;

/**
 * Refactoring history resource mapping for the Java model provider.
 *
 * @since 3.2
 */
public final class JavaRefactoringHistoryResourceMapping extends AbstractRefactoringHistoryResourceMapping {

	/**
	 * Creates a new refactoring history resource mapping.
	 *
	 * @param history
	 *            the refactoring history
	 */
	public JavaRefactoringHistoryResourceMapping(final RefactoringHistory history) {
		super(history);
	}

	@Override
	public String getModelProviderId() {
		return JavaModelProvider.JAVA_MODEL_PROVIDER_ID;
	}
}