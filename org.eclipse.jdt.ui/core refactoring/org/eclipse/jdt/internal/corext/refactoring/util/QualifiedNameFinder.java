/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others. All
 * rights reserved. This program and the accompanying materials are made
 * available under the terms of the Common Public License v1.0 which accompanies
 * this distribution, and is available at http://www.eclipse.org/legal/cpl-v10.
 * html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.search.internal.core.text.ITextSearchResultCollector;
import org.eclipse.search.internal.core.text.TextSearchEngine;
import org.eclipse.search.internal.core.text.TextSearchScope;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextChange;
import org.eclipse.jdt.internal.corext.refactoring.changes.TextFileChange;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;

public class QualifiedNameFinder {
	
	private class ResultCollector implements ITextSearchResultCollector {
		
		private String fNewValue;
		
		public ResultCollector(String newValue,  IProgressMonitor monitor) {
			fNewValue= newValue;
			fProgressMonitor= monitor;
		}
		
		public void aboutToStart() throws CoreException {
			// do nothing;
		}

		public void accept(IResource resource, String line, int start, int length, int lineNumber) throws CoreException {
			if (resource.getType() != IResource.FILE)
				return;
			// Make sure we don't change Compilation Units
			IFile file= (IFile)resource;
			IJavaElement element= JavaCore.create(file);
			if (element != null && element.exists())
				return;
			TextChange change= (TextChange)fChanges.get(file);
			if (change == null) {
				change= new TextFileChange(resource.getName(), file);
				fChanges.put(resource, change);
				fChangesList.add(change);
			}
			change.addTextEdit("Update fully qualified name", SimpleTextEdit.createReplace(start, length, fNewValue));
		}

		public void done() throws CoreException {
			// do nothing;
		}

		public IProgressMonitor getProgressMonitor() {
			return fProgressMonitor;
		}
	}
		
	private Map fChanges;
	private List fChangesList;
	private IProgressMonitor fProgressMonitor;
	
	public QualifiedNameFinder() {
		fChanges= new HashMap();
		fChangesList= new ArrayList();
	}
	
	public void process(String pattern, String newValue, String filePatterns, IProject root, IProgressMonitor monitor) throws JavaModelException {
		Assert.isNotNull(pattern);
		Assert.isNotNull(newValue);
		Assert.isNotNull(filePatterns);
		Assert.isNotNull(root);
		if (monitor == null)
			monitor= new NullProgressMonitor();
		ResultCollector collector= new ResultCollector(newValue, monitor);
		TextSearchEngine engine= new TextSearchEngine();
		engine.search(ResourcesPlugin.getWorkspace(), pattern, "", createScope(filePatterns, root), collector);
	}
	
	public TextChange[] getAllChanges() {
		return (TextChange[])fChangesList.toArray(new TextChange[fChangesList.size()]);
	}
	
	public IFile[] getAllFiles() {
		Set keys= fChanges.keySet();
		return (IFile[])keys.toArray(new IFile[keys.size()]);			
	}
	
	private static TextSearchScope createScope(String filePatterns, IProject root) throws JavaModelException {
		String[] patterns= splitFilePatterns(filePatterns);
		TextSearchScope result= new TextSearchScope("");
		result.add(root);
		addReferencingProjects(result, root);
		for (int i= 0; i < patterns.length; i++) {
			result.addExtension(patterns[i]);
		}
		return result;
	}
	
	private static String[] splitFilePatterns(String filePatterns) {
		List result= new ArrayList();
		StringTokenizer tokenizer= new StringTokenizer(filePatterns, ",");
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
