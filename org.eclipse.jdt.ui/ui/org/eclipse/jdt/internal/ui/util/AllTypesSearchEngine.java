/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ITypeNameRequestor;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

/**
 * Search for all types in the workspace. Instead of returning objects of type <code>IType</code>
 * the methods of this class returns a list of the lightweight objects <code>TypeInfo</code>.
 */
public class AllTypesSearchEngine {

	private IWorkspace fWorkspace;

	public AllTypesSearchEngine(IWorkspace workspace) {
		Assert.isNotNull(workspace);
		fWorkspace= workspace;
	}

	/**
	 * Search for type in the given search scope.
	 * @param style a combination of <code>IJavaElementSearchConstants</code> flags
	 */
	public List searchTypes(IRunnableContext context, final IJavaSearchScope scope, final int style) {
		final List typesFound= new ArrayList(2000);
		
		if (TypeCache.canReuse(style, scope)){
			typesFound.addAll(TypeCache.getCachedTypes());
			return typesFound;
		}
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				searchTypes(typesFound, scope, style, pm);
			}
		};
		
		try {
			context.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			Throwable t= e.getTargetException();
			if (t instanceof Error)
				throw (Error)t;
			if (t instanceof RuntimeException)
				throw (RuntimeException)t;	
			ExceptionHandler.handle(e, "Exception", "Unexpected exception. See log for details.");
		} catch (InterruptedException e) {
		}
		
		return typesFound;	
	}
	
	/**
	 * Search for type in the given search scope.
	 * @param style a combination of <code>IJavaElementSearchConstants</code> flags
	 */		
	public void searchTypes(List typesFound, IJavaSearchScope scope, int style, IProgressMonitor pm) {
		if (TypeCache.canReuse(style, scope)){
			typesFound.addAll(TypeCache.getCachedTypes());
			pm.done();
			return;
		}
		TypeCache.flush();
		TypeCache.setConfiguration(style);
		doSearchTypes(typesFound, scope, style, pm);

		TypeCache.setCachedTypes(createCopy(typesFound));
		TypeCache.registerIfNecessary();
	}

	private static List createCopy(List list) {
		List newList= new ArrayList(list.size());
		newList.addAll(list);
		return newList;
	}
	
	private void doSearchTypes(List typesFound, IJavaSearchScope scope, int style, IProgressMonitor pm) {
		Assert.isTrue((style & IJavaElementSearchConstants.CONSIDER_TYPES) != 0);
				
		try {
			new SearchEngine().searchAllTypeNames(
				fWorkspace,
				null,
				null,
				IJavaSearchConstants.PATTERN_MATCH,
				IJavaSearchConstants.CASE_INSENSITIVE,
				computeKind(style),
				scope,
				new TypeInfoRequestor(typesFound),
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH,
				pm);
		} catch(JavaModelException e){
			ExceptionHandler.handle(e,  "Exception", "Unexpected exception. See log for details.");
		}
	}
	
	private static int computeKind(int style) {
		int kind= IJavaSearchConstants.INTERFACE;
		if ((style & IJavaElementSearchConstants.CONSIDER_INTERFACES) != 0 && (style & IJavaElementSearchConstants.CONSIDER_CLASSES) != 0) 
			kind= IJavaSearchConstants.TYPE;
		else if ((style & IJavaElementSearchConstants.CONSIDER_CLASSES) != 0)
			kind= IJavaSearchConstants.CLASS;
		return kind;
	}
}