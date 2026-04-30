/*******************************************************************************
 * Copyright (c) 2026 Eclipse Foundation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Arcadiy Ivanov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.callhierarchy;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchDocument;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;

/**
 * Test search participant for call hierarchy tests. Registered via plugin.xml
 * for the {@code .testlang} file extension.
 *
 * <p>Tests control its behavior via static fields set before invoking the
 * call hierarchy API.
 */
public class TestCallHierarchyParticipant extends SearchParticipant {

	/** Callees to return from {@link #locateCallees}. Set by tests before use. */
	public static SearchMatch[] calleesToReturn = new SearchMatch[0];

	/** Number of times {@link #locateCallees} was called. */
	public static int locateCalleesCallCount = 0;

	public static void reset() {
		calleesToReturn = new SearchMatch[0];
		locateCalleesCallCount = 0;
	}

	@Override
	public SearchDocument getDocument(String documentPath) {
		return new SearchDocument(documentPath, this) {
			@Override
			public byte[] getByteContents() {
				return new byte[0];
			}

			@Override
			public char[] getCharContents() {
				return new char[0];
			}

			@Override
			public String getEncoding() {
				return "UTF-8"; //$NON-NLS-1$
			}
		};
	}

	@Override
	public void indexDocument(SearchDocument document, IPath indexLocation) {
		// no-op
	}

	@Override
	public void locateMatches(SearchDocument[] documents, SearchPattern pattern,
			IJavaSearchScope scope, SearchRequestor requestor,
			IProgressMonitor monitor) throws CoreException {
		// no-op
	}

	@Override
	public IPath[] selectIndexes(SearchPattern query, IJavaSearchScope scope) {
		return new IPath[0];
	}

	@Override
	public SearchMatch[] locateCallees(IMember caller, SearchDocument document,
			IProgressMonitor monitor) throws CoreException {
		locateCalleesCallCount++;
		return calleesToReturn;
	}
}
