/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.base;

import org.eclipse.core.runtime.Assert;

import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import org.eclipse.jdt.core.ISourceRange;

/**
 * A Java string context can be used to annotate a </code>RefactoringStatusEntry<code>
 * with detailed information about an error detected in Java source code represented
 * by a string.
 */
public class JavaStringStatusContext extends RefactoringStatusContext {

	private String fSource;
	private ISourceRange fSourceRange;

	/**
	 * Creates a new <code>JavaStringStatusContext</code>.
	 *
	 * @param source the source code containing the error
	 * @param range a source range inside <code>source</code> or
	 *  <code>null</code> if no special source range is known.
	 */
	public JavaStringStatusContext(String source, ISourceRange range){
		Assert.isNotNull(source);
		fSource= source;
		fSourceRange= range;
	}

	public String getSource() {
		return fSource;
	}

	public ISourceRange getSourceRange() {
		return fSourceRange;
	}

	@Override
	public Object getCorrespondingElement() {
		return null;
	}
}
