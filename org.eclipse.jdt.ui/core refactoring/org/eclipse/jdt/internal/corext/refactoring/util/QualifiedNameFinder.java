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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.search.internal.core.text.ITextSearchResultCollector;
import org.eclipse.search.internal.core.text.TextSearchEngine;
import org.eclipse.search.internal.core.text.TextSearchScope;

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
			TextChange change= (TextChange)fChanges.get(resource);
			if (change == null) {
				change= new TextFileChange(resource.getName(), (IFile)resource);
				fChanges.put(resource, change);
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
	
	private String fFilePatterns;
	private String fPattern;
	private String fNewValue;
	
	private Map fChanges;
	private IProgressMonitor fProgressMonitor;
	
	
	public QualifiedNameFinder(String filePatterns, String pattern, String newValue) {
		Assert.isNotNull(filePatterns);
		Assert.isNotNull(pattern);
		Assert.isNotNull(newValue);
		fFilePatterns= filePatterns;
		fPattern= pattern;
		fNewValue= newValue;
		fChanges= new HashMap();
	}
	
	public void process(IProgressMonitor monitor) {
		ResultCollector collector= new ResultCollector(fNewValue, monitor);
		TextSearchEngine engine= new TextSearchEngine();
		engine.search(ResourcesPlugin.getWorkspace(), fPattern, "", createScope(), collector);
	}
	
	public TextChange[] getAllChanges() {
		Collection values= fChanges.values();
		return (TextChange[])values.toArray(new TextChange[values.size()]);
	}
	
	public IFile[] getAllFiles() {
		Set keys= fChanges.keySet();
		return (IFile[])keys.toArray(new IFile[keys.size()]);			
	}
	
	private TextSearchScope createScope() {
		String[] patterns= splitFilePatterns();
		TextSearchScope result= TextSearchScope.newWorkspaceScope();
		for (int i= 0; i < patterns.length; i++) {
			result.addExtension(patterns[i]);
		}
		return result;
	}
	
	private String[] splitFilePatterns() {
		List result= new ArrayList();
		StringTokenizer tokenizer= new StringTokenizer(fFilePatterns, ",");
		while(tokenizer.hasMoreTokens())
			result.add(tokenizer.nextToken());
		return (String[]) result.toArray(new String[result.size()]);	
	}	
}
