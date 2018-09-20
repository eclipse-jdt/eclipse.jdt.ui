/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.search;

import org.eclipse.search.ui.NewSearchUI;

import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.SharedASTProviderCore;

import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder;


public final class FindOccurrencesEngine {

	public static FindOccurrencesEngine create(IOccurrencesFinder finder) {
		return new FindOccurrencesEngine(finder);
	}

	private IOccurrencesFinder fFinder;

	private FindOccurrencesEngine(IOccurrencesFinder finder) {
		if (finder == null)
			throw new IllegalArgumentException();
		fFinder= finder;
	}

	private String run(CompilationUnit astRoot, int offset, int length) {
		String message= fFinder.initialize(astRoot, offset, length);
		if (message != null)
			return message;

		performNewSearch(fFinder, astRoot.getTypeRoot());
		return null;
	}

	public String run(ITypeRoot input, int offset, int length) throws JavaModelException {
		if (input.getSourceRange() == null) {
			return SearchMessages.FindOccurrencesEngine_noSource_text;
		}

		final CompilationUnit root= SharedASTProviderCore.getAST(input, SharedASTProviderCore.WAIT_YES, null);
		if (root == null) {
			return SearchMessages.FindOccurrencesEngine_cannotParse_text;
		}
		return run(root, offset, length);
	}

	private void performNewSearch(IOccurrencesFinder finder, ITypeRoot element) {
		NewSearchUI.runQueryInBackground(new OccurrencesSearchQuery(finder, element));
	}
}
