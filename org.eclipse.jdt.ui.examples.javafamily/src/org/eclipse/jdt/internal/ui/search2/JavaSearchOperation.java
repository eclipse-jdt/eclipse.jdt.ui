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
package org.eclipse.jdt.internal.ui.search2;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jsp.*;
import org.eclipse.jsp.JspTypeQuery;

import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.search.PrettySignature;


public class JavaSearchOperation extends WorkspaceModifyOperation {
	
	private IWorkspace fWorkspace;
	private IJavaElement fElementPattern;
	private int fLimitTo;
	private String fStringPattern;
	private boolean fIsCaseSensitive;
	private int fSearchFor;
	private IJavaSearchScope fScope;
	private String fScopeDescription;
	private JavaSearchResultCollector fCollector;
	
	protected JavaSearchOperation(
				IWorkspace workspace,
				int limitTo,
				IJavaSearchScope scope,
				String scopeDescription,
				JavaSearchResultCollector collector) {
		super(null);
		fWorkspace= workspace;
		fLimitTo= limitTo;
		fScope= scope;
		fScopeDescription= scopeDescription;
		fCollector= collector;
		fCollector.setOperation(this);
	}
	
	public JavaSearchOperation(
				IWorkspace workspace,
				IJavaElement pattern,
				int limitTo,
				IJavaSearchScope scope,
				String scopeDescription,
				JavaSearchResultCollector collector) {
		this(workspace, limitTo, scope, scopeDescription, collector);
		fElementPattern= pattern;
	}
	
	public JavaSearchOperation(
				IWorkspace workspace,
				String pattern,
				boolean caseSensitive,
				int searchFor, 
				int limitTo,
				IJavaSearchScope scope,
				String scopeDescription,
				JavaSearchResultCollector collector) {
		this(workspace, limitTo, scope, scopeDescription, collector);
		fStringPattern= pattern;
		fIsCaseSensitive= caseSensitive;
		fSearchFor= searchFor;
	}
	
	protected void execute(IProgressMonitor monitor) throws CoreException {
		fCollector.setProgressMonitor(monitor);
		
		// A hack to temporarily disable the aboutToStart and done methods
		IJavaSearchResultCollector collector= new IJavaSearchResultCollector() {
			public void aboutToStart() {
				// empty implementation
			}
			public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws CoreException {
				fCollector.accept(resource, start, end, enclosingElement, accuracy);
			}
			public void done() {
				// empty implementation
			}
			public IProgressMonitor getProgressMonitor() {
				return fCollector.getProgressMonitor();
			}
		};
		
		fCollector.aboutToStart();

		// Also search working copies
		SearchEngine engine= new SearchEngine(JavaUI.getSharedWorkingCopiesOnClasspath());
		
		if (fElementPattern != null)
			engine.search(fWorkspace, fElementPattern, fLimitTo, fScope, collector);
		else
			engine.search(fWorkspace, SearchEngine.createSearchPattern(fStringPattern, fSearchFor, fLimitTo, fIsCaseSensitive), fScope, collector);

		if (fElementPattern instanceof IType)
			JspSearchEngine.search(collector, new JspTypeQuery((IType)fElementPattern), monitor);
			
		fCollector.done();
	}

	String getSingularLabel() {
		String desc= null;
		if (fElementPattern != null) {
			if (fLimitTo == IJavaSearchConstants.REFERENCES
			&& fElementPattern.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)fElementPattern);
			else
				desc= fElementPattern.getElementName();
			if ("".equals(desc) && fElementPattern.getElementType() == IJavaElement.PACKAGE_FRAGMENT) 
				desc= "(default package)"; 
		}
		else
			desc= fStringPattern;

		String[] args= new String[] {desc, fScopeDescription}; //$NON-NLS-1$
		switch (fLimitTo) {
			case IJavaSearchConstants.IMPLEMENTORS:
				return "{0} - 1 Implementor in {1}"; 
			case IJavaSearchConstants.DECLARATIONS:
				return "{0} - 1 Declaration in {1}"; 
			case IJavaSearchConstants.REFERENCES:
				return "{0} - 1 Reference in {1}"; 
			case IJavaSearchConstants.ALL_OCCURRENCES:
				return "{0} - 1 Occurrence in {1}"; 
			case IJavaSearchConstants.READ_ACCESSES:
				return "{0} - 1 Read Reference in {1}"; 
			case IJavaSearchConstants.WRITE_ACCESSES:
				return "{0} - 1 Write Reference in {1}"; 
			default:
				return "{0} - 1 Occurrence in {1}";
		}
	}

	String getPluralLabelPattern() {
		String desc= null;
		if (fElementPattern != null) {
			if (fLimitTo == IJavaSearchConstants.REFERENCES
			&& fElementPattern.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)fElementPattern);
			else
				desc= fElementPattern.getElementName();
			if ("".equals(desc) && fElementPattern.getElementType() == IJavaElement.PACKAGE_FRAGMENT) 
				desc= "(default package)"; 
		}
		else
			desc= fStringPattern;

		String[] args= new String[] {desc, "{0}", fScopeDescription}; 
		switch (fLimitTo) {
			case IJavaSearchConstants.IMPLEMENTORS:
				return "{0} - {1} Implementors in {2}"; 
			case IJavaSearchConstants.DECLARATIONS:
				return "{0} - {1} Declarations in {2}"; 
			case IJavaSearchConstants.REFERENCES:
				return "{0} - {1} References in {2}"; 
			case IJavaSearchConstants.ALL_OCCURRENCES:
				return "{0} - {1} Occurrences in {2}"; 
			case IJavaSearchConstants.READ_ACCESSES:
				return "{0} - {1} Read References in {2}"; 
			case IJavaSearchConstants.WRITE_ACCESSES:
				return "{0} - {1} Write References in {2}"; 
			default:
				return "{0} - {1} Occurrences in {2}";
		}
	}
	
	ImageDescriptor getImageDescriptor() {
		if (fLimitTo == IJavaSearchConstants.IMPLEMENTORS || fLimitTo == IJavaSearchConstants.DECLARATIONS)
			return JavaPluginImages.DESC_OBJS_SEARCH_DECL;
		else
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}
}
