package org.eclipse.jdt.internal.ui.search;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;

public class JavaSearchOperation extends WorkspaceModifyOperation {
	
	private IWorkspace fWorkspace;
	private IJavaElement fElementPattern;
	private int fLimitTo;
	private String fStringPattern;
	private int fSearchFor;
	private IJavaSearchScope fScope;
	private JavaSearchResultCollector fCollector;
	
	protected JavaSearchOperation(IWorkspace workspace, int limitTo,
			IJavaSearchScope scope, JavaSearchResultCollector collector) {
		fWorkspace= workspace;
		fLimitTo= limitTo;
		fScope= scope;
		fCollector= collector;
		fCollector.setOperation(this);
	}
	
	public JavaSearchOperation(IWorkspace workspace, IJavaElement pattern, int limitTo,
			IJavaSearchScope scope, JavaSearchResultCollector collector) {
		this(workspace, limitTo, scope, collector);
		fElementPattern= pattern;
	}
	
	public JavaSearchOperation(IWorkspace workspace, String pattern, int searchFor, 
			int limitTo, IJavaSearchScope scope, JavaSearchResultCollector collector) {
		this(workspace, limitTo, scope, collector);
		fStringPattern= pattern;
		fSearchFor= searchFor;
	}
	
	protected void execute(IProgressMonitor monitor) throws CoreException {
		fCollector.setProgressMonitor(monitor);
		SearchEngine engine= new SearchEngine();
		if (fElementPattern != null)
			engine.search(fWorkspace, fElementPattern, fLimitTo, fScope, fCollector);
		else
			engine.search(fWorkspace, fStringPattern, fSearchFor, fLimitTo, fScope, fCollector);
	}

	String getDescription() {
		String desc= null;
		if (fElementPattern != null) {
			if (fLimitTo == IJavaSearchConstants.REFERENCES
			&& fElementPattern.getElementType() == IJavaElement.METHOD)
				desc= PrettySignature.getUnqualifiedMethodSignature((IMethod)fElementPattern);
			else
				desc= fElementPattern.getElementName();
		}
		else
			desc= fStringPattern;
		switch (fLimitTo) {
			case IJavaSearchConstants.IMPLEMENTORS:
				return desc + JavaPlugin.getResourceString("JavaSearchOperation.implementorsPostfix");
			case IJavaSearchConstants.DECLARATIONS:
				return desc + JavaPlugin.getResourceString("JavaSearchOperation.declarationsPostfix");
			case IJavaSearchConstants.REFERENCES:
				return desc + JavaPlugin.getResourceString("JavaSearchOperation.referencesPostfix");
			case IJavaSearchConstants.ALL_OCCURRENCES:
				return desc + JavaPlugin.getResourceString("JavaSearchOperation.occurrencesPostfix");
			default:
				return "";
		}
	}
	
	ImageDescriptor getImageDescriptor() {
		if (fLimitTo == IJavaSearchConstants.IMPLEMENTORS || fLimitTo == IJavaSearchConstants.DECLARATIONS)
			return JavaPluginImages.DESC_OBJS_SEARCH_DECL;
		else
			return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}
}