/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.search.internal.core.text.ITextSearchResultCollector;
import org.eclipse.search.internal.core.text.MatchLocator;
import org.eclipse.search.internal.core.text.TextSearchEngine;
import org.eclipse.search.internal.core.text.TextSearchScope;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChangeCompatibility;
import org.eclipse.ltk.core.refactoring.TextChange;

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

		public void accept(IResourceProxy proxy, String line, int start, int length, int lineNumber) throws CoreException {
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
				RefactoringCoreMessages.getString("QualifiedNameFinder.update_name"),  //$NON-NLS-1$
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
	
	public static void process(QualifiedNameSearchResult result, String pattern, String newValue, String filePatterns, IProject root, IProgressMonitor monitor) throws JavaModelException {
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
		engine.search(ResourcesPlugin.getWorkspace(), 
			createScope(filePatterns, root), false,
			collector, new MatchLocator(pattern, true, false)); //$NON-NLS-1$
	}
	
	private static TextSearchScope createScope(String filePatterns, IProject root) throws JavaModelException {
		String[] patterns= splitFilePatterns(filePatterns);
		TextSearchScope result= new TextSearchScope(""); //$NON-NLS-1$
		result.add(root);
		addReferencingProjects(result, root);
		for (int i= 0; i < patterns.length; i++) {
			result.addExtension(patterns[i]);
		}
		return result;
	}
	
	private static String[] splitFilePatterns(String filePatterns) {
		List result= new ArrayList();
		StringTokenizer tokenizer= new StringTokenizer(filePatterns, ","); //$NON-NLS-1$
		while(tokenizer.hasMoreTokens())
			result.add(tokenizer.nextToken().trim());
		return (String[]) result.toArray(new String[result.size()]);	
	}
	
	private static void addReferencingProjects(TextSearchScope scope, IProject root) {
		IProject[] projects= root.getReferencingProjects();
		for (int i= 0; i < projects.length; i++) {
			IProject project= projects[i];
			if (!scope.encloses(project)) {
				scope.add(project);
				addReferencingProjects(scope, project);
			}
		}
	}
}
