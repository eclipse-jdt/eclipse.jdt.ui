/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Red Hat Inc - Reorganization, Abstraction
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.search;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchMatchAccess;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.core.text.TextSearchScope;

import org.eclipse.jdt.core.manipulation.internal.search.ITextSearchCollector;
import org.eclipse.jdt.core.manipulation.internal.search.ITextSearchMatchResult;
import org.eclipse.jdt.core.manipulation.internal.search.ITextSearchRunner;

public class UISearchRunner implements ITextSearchRunner {
	@Override
	public void search(IFile[] files, ITextSearchCollector collector, Pattern searchPattern,IProgressMonitor monitor) {
		TextSearchEngine engine= TextSearchEngine.create();
		engine.search(files, wrapCollector(collector), searchPattern, monitor);
	}

	@Override
	public void search(IResource[] roots, Pattern filePatterns, boolean visitDerivedResources, ITextSearchCollector collector, Pattern searchPattern, IProgressMonitor monitor) {
		TextSearchEngine engine= TextSearchEngine.create();
		TextSearchScope scope = TextSearchScope.newSearchScope(roots, filePatterns, visitDerivedResources);
		engine.search(scope, wrapCollector(collector), searchPattern, monitor);
	}

	private TextSearchRequestor wrapCollector(final ITextSearchCollector delegate) {
		return new TextSearchRequestor() {
			@Override
			public void beginReporting() {
				delegate.beginReporting();
			}

			@Override
			public void endReporting() {
				delegate.endReporting();
			}

			@Override
			public boolean acceptFile(IFile file) throws CoreException {
				return delegate.acceptFile(file);
			}

			@Override
			public void flushMatches(IFile file) {
				delegate.flushMatches(file);
			}

			@Override
			public boolean reportBinaryFile(IFile file) {
				return delegate.reportBinaryFile(file);
			}

			@Override
			public boolean acceptPatternMatch(TextSearchMatchAccess matchAccess) throws CoreException {
				return delegate.acceptPatternMatch(wrapResult(matchAccess));
			}

			@Override
			public boolean canRunInParallel() {
				return delegate.canRunInParallel();
			}

			private ITextSearchMatchResult wrapResult(final TextSearchMatchAccess matchAccess) {
				return new ITextSearchMatchResult() {

					@Override
					public IFile getFile() {
						return matchAccess.getFile();
					}

					@Override
					public int getMatchOffset() {
						return matchAccess.getMatchOffset();
					}

					@Override
					public int getMatchLength() {
						return matchAccess.getMatchLength();
					}

					@Override
					public int getFileContentLength() {
						return matchAccess.getFileContentLength();
					}

					@Override
					public char getFileContentChar(int offset) {
						return matchAccess.getFileContentChar(offset);
					}

					@Override
					public String getFileContent(int offset, int length) {
						return matchAccess.getFileContent(offset, length);
					}

				};
			}
		};
	}

}
