/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Search for all types in the workspace. Instead of returning objects of type <code>IType</code>
 * the methods of this class return a list of the lightweight objects <code>TypeRef</code>.
 */
public class AllTypesSearchEngine extends SearchEngine {

	private IWorkspace fWorkspace;

	public AllTypesSearchEngine(IWorkspace workspace) {
		fWorkspace= workspace;
	}

	/**
	 * Search for type in the given search scope.
	 */
	public List searchTypes(IRunnableContext context, final IJavaSearchScope scope, final int style) {
		final List typesFound= new ArrayList(1000);
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				searchTypes(typesFound, scope, style, pm);
			}
		};
		
		try {
			context.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			JavaPlugin.log(e.getTargetException());
		} catch (InterruptedException e) {
		}
		
		return typesFound;	
	}
	
	public void searchTypes(List typesFound, IJavaSearchScope scope, int style, IProgressMonitor pm) {
		Assert.isTrue((style & IJavaElementSearchConstants.CONSIDER_TYPES) != 0);
				
		int kind= IJavaSearchConstants.INTERFACE;
		if ((style & IJavaElementSearchConstants.CONSIDER_INTERFACES) != 0 && (style & IJavaElementSearchConstants.CONSIDER_CLASSES) != 0) 
			kind= IJavaSearchConstants.TYPE;
		else if ((style & IJavaElementSearchConstants.CONSIDER_CLASSES) != 0)
			kind= IJavaSearchConstants.CLASS;
		
		ITypeNameRequestor requestor= new TypeRefRequestor(typesFound);
		try {
			searchAllTypeNames(
				fWorkspace,
				null,
				null,
				IJavaSearchConstants.PATTERN_MATCH,
				IJavaSearchConstants.CASE_INSENSITIVE,
				kind,
				scope,
				requestor,
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
				pm);
		} catch(CoreException e){
			JavaPlugin.log(e);
		}
	}
}