/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.JavaPlugin;

public class MainMethodSearchEngine{
	
	private static class MethodCollector implements IJavaSearchResultCollector {
			private List fResult;
			private int fStyle;
			private IProgressMonitor fProgressMonitor;

			public MethodCollector(List result, int style, IProgressMonitor progressMonitor) {
				Assert.isNotNull(result);
				fResult= result;
				fStyle= style;
				fProgressMonitor= progressMonitor;
			}

			private boolean considerExternalJars() {
				return (fStyle & IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS) != 0;
			}
					
			private boolean considerBinaries() {
				return (fStyle & IJavaElementSearchConstants.CONSIDER_BINARIES) != 0;
			}		
			
			public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) {
				if (enclosingElement instanceof IMethod) { // defensive code
					try {
						IMethod curr= (IMethod) enclosingElement;
						if (curr.isMainMethod()) {
							if (!considerExternalJars()) {
								IPackageFragmentRoot root= JavaModelUtil.getPackageFragmentRoot(curr);
								if (root == null || root.isArchive()) {
									return;
								}
							}
							if (!considerBinaries() && curr.isBinary()) {
								return;
							}
							fResult.add(curr.getDeclaringType());
						}
					} catch (JavaModelException e) {
						JavaPlugin.log(e.getStatus());
					}
				}
			}
							
			public IProgressMonitor getProgressMonitor() {
				return fProgressMonitor;
			}
			
			public void aboutToStart() {
			}
			
			public void done() {
			}
	}

	/**
	 * Searches for all main methods in the given scope.
	 * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
	 * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
	 */	
	public IType[] searchMainMethods(IProgressMonitor pm, IJavaSearchScope scope, int style) throws JavaModelException {
		List typesFound= new ArrayList(200);
		
		IJavaSearchResultCollector collector= new MethodCollector(typesFound, style, pm);				
		new SearchEngine().search(JavaPlugin.getWorkspace(), "main(String[]) void", IJavaSearchConstants.METHOD,  //$NON-NLS-1$
			IJavaSearchConstants.DECLARATIONS, scope, collector); //$NON-NLS-1$
			
		return (IType[]) typesFound.toArray(new IType[typesFound.size()]);
	}
	
	
	
	/**
	 * Searches for all main methods in the given scope.
	 * Valid styles are IJavaElementSearchConstants.CONSIDER_BINARIES and
	 * IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS
	 */
	public IType[] searchMainMethods(IRunnableContext context, final IJavaSearchScope scope, final int style) throws InvocationTargetException, InterruptedException  {
		int allFlags=  IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS | IJavaElementSearchConstants.CONSIDER_BINARIES;
		Assert.isTrue((style | allFlags) == allFlags);
		
		final IType[][] res= new IType[1][];
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					res[0]= searchMainMethods(pm, scope, style);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		context.run(true, true, runnable);
		
		return res[0];
	}
			
}