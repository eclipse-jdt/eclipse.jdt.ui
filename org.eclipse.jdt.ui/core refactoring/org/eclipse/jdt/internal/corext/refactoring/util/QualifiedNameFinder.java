/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;

import org.eclipse.search.internal.core.SearchScope;
import org.eclipse.search.internal.core.text.ITextSearchResultCollector;
import org.eclipse.search.internal.core.text.MatchLocator;
import org.eclipse.search.internal.core.text.TextSearchEngine;

import org.eclipse.ltk.core.refactoring.TextChange;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;

public class QualifiedNameFinder {
	
	private static class ResultCollector implements ITextSearchResultCollector {
		
		private String fNewValue;
		private IProgressMonitor fProgressMonitor;
		private QualifiedNameSearchResult fResult;
		
		public ResultCollector(QualifiedNameSearchResult result, String newValue, IProgressMonitor monitor) {
			fResult= result;
			fNewValue= newValue;
			fProgressMonitor= monitor;
		}
		
		public void aboutToStart() throws CoreException {
			// do nothing;
		}

		public void accept(IResourceProxy proxy, int start, int length) throws CoreException {
			if (proxy.getType() != IResource.FILE)
				return;
			// Make sure we don't change Compilation Units
			IFile file= (IFile)proxy.requestResource();
			IJavaElement element= JavaCore.create(file);
			if ((element != null && element.exists()))
				return;
			TextChange change= fResult.getChange(file);
			TextChangeCompatibility.addTextEdit(
				change, 
				RefactoringCoreMessages.QualifiedNameFinder_update_name,  
				new ReplaceEdit(start, length, fNewValue));
		}

		public void done() throws CoreException {
			// do nothing;
		}

		public IProgressMonitor getProgressMonitor() {
			return fProgressMonitor;
		}
	}
		
	public QualifiedNameFinder() {
	}
	
	public static void process(QualifiedNameSearchResult result, String pattern, String newValue, String filePatterns, IProject root, IProgressMonitor monitor) {
		if (filePatterns == null || filePatterns.length() == 0) {
			// Eat progress.
			monitor.beginTask("", 1); //$NON-NLS-1$
			monitor.worked(1);
			return;
		}
		Assert.isNotNull(pattern);
		Assert.isNotNull(newValue);
		Assert.isNotNull(root);
		if (monitor == null)
			monitor= new NullProgressMonitor();
		ResultCollector collector= new ResultCollector(result, newValue, monitor);
		TextSearchEngine engine= new TextSearchEngine();
		engine.search(createScope(filePatterns, root), false,
			collector, new MatchLocator(pattern, true, false));
	}
	
	private static SearchScope createScope(String filePatterns, IProject root) {
		HashSet res= new HashSet();
		res.add(root);
		addReferencingProjects(root, res);
		IResource[] resArr= (IResource[]) res.toArray(new IResource[res.size()]);
		
		SearchScope result= SearchScope.newSearchScope("", resArr); //$NON-NLS-1$
		addFilePatterns(filePatterns, result);
		return result;
	}
	
	private static void addFilePatterns(String filePatterns, SearchScope scope) {
		StringTokenizer tokenizer= new StringTokenizer(filePatterns, ","); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			scope.addFileNamePattern(tokenizer.nextToken().trim());
		}
	}
	
	private static void addReferencingProjects(IProject root, Set res) {
		IProject[] projects= root.getReferencingProjects();
		for (int i= 0; i < projects.length; i++) {
			IProject project= projects[i];
			if (res.add(project)) {
				addReferencingProjects(project, res);
			}
		}
	}
}
