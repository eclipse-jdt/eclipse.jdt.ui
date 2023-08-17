/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Terry Parker <tparker@google.com> (Google Inc.) - Bug 458852 - Speed up JDT text searches by supporting parallelism in its TextSearchRequestors
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.core.filebuffers.FileBuffers;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.ltk.core.refactoring.GroupCategory;
import org.eclipse.ltk.core.refactoring.GroupCategorySet;
import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.manipulation.internal.search.ITextSearchCollector;
import org.eclipse.jdt.core.manipulation.internal.search.ITextSearchMatchResult;
import org.eclipse.jdt.core.manipulation.internal.search.ITextSearchRunner;
import org.eclipse.jdt.core.manipulation.internal.search.TextSearchAssistantSingleton;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;

import org.eclipse.jdt.internal.ui.util.PatternConstructor;

public class QualifiedNameFinder {

	private static final GroupCategorySet QUALIFIED_NAMES= new GroupCategorySet(
		new GroupCategory("org.eclipse.jdt.internal.corext.qualifiedNames", //$NON-NLS-1$
			RefactoringCoreMessages.QualifiedNameFinder_qualifiedNames_name,
			RefactoringCoreMessages.QualifiedNameFinder_qualifiedNames_description));

	private static class ResultCollector implements ITextSearchCollector {

		private final String fNewValue;
		private final QualifiedNameSearchResult fResult;

		public ResultCollector(QualifiedNameSearchResult result, String newValue) {
			fResult= result;
			fNewValue= newValue;
		}

		@Override
		public boolean canRunInParallel() {
			return true;
		}

		@Override
		public boolean acceptFile(IFile file) throws CoreException {
			IJavaElement element= JavaCore.create(file);
			if ((element != null && element.exists()))
				return false;

			// Only touch text files (see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=114153 ):
			if (! FileBuffers.getTextFileBufferManager().isTextFileLocation(file.getFullPath(), false))
				return false;

			IPath path= file.getProjectRelativePath();
			String segment= path.segment(0);
			if (segment != null && (segment.startsWith(".refactorings") || segment.startsWith(".deprecations"))) //$NON-NLS-1$ //$NON-NLS-2$
				return false;

			return true;
		}

		@Override
		public boolean acceptPatternMatch(ITextSearchMatchResult matchAccess) throws CoreException {
			int start= matchAccess.getMatchOffset();
			int length= matchAccess.getMatchLength();

			// skip embedded FQNs (bug 130764):
			if (start > 0) {
				char before= matchAccess.getFileContentChar(start - 1);
				if (before == '.' || Character.isJavaIdentifierPart(before))
					return true;
			}
			int fileContentLength= matchAccess.getFileContentLength();
			int end= start + length;
			if (end < fileContentLength) {
				char after= matchAccess.getFileContentChar(end);
				if (Character.isJavaIdentifierPart(after))
					return true;
			}

			IFile file= matchAccess.getFile();
			synchronized(fResult) {
				TextChange change= fResult.getChange(file);
				TextChangeCompatibility.addTextEdit(
					change,
					RefactoringCoreMessages.QualifiedNameFinder_update_name,
					new ReplaceEdit(start, length, fNewValue), QUALIFIED_NAMES);
			}

			return true;
		}

		@Override
		public void beginReporting() {
			// TODO Auto-generated method stub

		}

		@Override
		public void endReporting() {
			// TODO Auto-generated method stub

		}

		@Override
		public void flushMatches(IFile file) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean reportBinaryFile(IFile file) {
			// TODO Auto-generated method stub
			return false;
		}
	}

	private QualifiedNameFinder() {
	}

	public static void process(QualifiedNameSearchResult result, String pattern, String newValue, String filePatterns, IProject root, IProgressMonitor monitor) {
		Assert.isNotNull(pattern);
		Assert.isNotNull(newValue);
		Assert.isNotNull(root);

		if (monitor == null)
			monitor= new NullProgressMonitor();

		if (filePatterns == null || filePatterns.length() == 0) {
			// Eat progress.
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.worked(1);
			return;
		}
		ResultCollector collector= new ResultCollector(result, newValue);
		IResource[] roots = getSearchProjects(root);
		ITextSearchRunner runner = TextSearchAssistantSingleton.getDefault().getRunner();
		if( runner != null ) {
			runner.search(roots, getFilePattern(filePatterns), false, collector, getSearchPattern(pattern), monitor);
		}
	}

	private static Pattern getSearchPattern(String searchPattern) {
		return PatternConstructor.createPattern(searchPattern, true, false);
	}

	private static Pattern getFilePattern(String filePatterns) {
		StringTokenizer tokenizer= new StringTokenizer(filePatterns, ","); //$NON-NLS-1$
		String[] filePatternArray= new String[tokenizer.countTokens()];
		int i= 0;
		while (tokenizer.hasMoreTokens()) {
			filePatternArray[i++]= tokenizer.nextToken().trim();
		}
		return PatternConstructor.createPattern(filePatternArray, true, false);
	}

	private static IResource[] getSearchProjects(IProject p) {
		HashSet<IProject> res= new HashSet<>();
		res.add(p);
		addReferencingProjects(p, res);
		IResource[] resArr= res.toArray(new IResource[res.size()]);
		return resArr;
	}

	private static void addReferencingProjects(IProject root, Set<IProject> res) {
		for (IProject project : root.getReferencingProjects()) {
			if (res.add(project)) {
				addReferencingProjects(project, res);
			}
		}
	}
}
