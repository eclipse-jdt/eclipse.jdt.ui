/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public class MainMethodSearchEngine extends SearchEngine {
	
	public List searchMethod(IRunnableContext context, final IJavaSearchScope scope, final int style) {
		final List typesFound= new ArrayList(200);
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				try {
					searchMethod(typesFound, scope, style, pm);
				} catch (JavaModelException e) {
					// ignore
				} catch (CoreException e) {
				}
			}
		};
		
		try {
			context.run(true, true, runnable);
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
		
		return typesFound;	
	}
	private List searchMethod(final List v, IJavaSearchScope scope, final int style, final IProgressMonitor pm) throws JavaModelException, CoreException {
				
		IJavaSearchResultCollector collector= new IJavaSearchResultCollector() {
			public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) {
				if (enclosingElement instanceof IMethod) {
					IMethod method= (IMethod)enclosingElement;
					if (JavaModelUtil.isMainMethod((IMethod)enclosingElement)) {
						// partial: fix for 
						// 1GBADLN: ITPJUI:WINNT - SH: Launch/Debug list with runnables not complete and bad to use
						// only add managed resources
						try {		
							if ((style & IJavaElementSearchConstants.CONSIDER_EXTERNAL_JARS) == 0 && enclosingElement.getUnderlyingResource() == null)
								return;
							if ((style & IJavaElementSearchConstants.CONSIDER_BINARIES) == 0 && method.isBinary())
								return;
							v.add(enclosingElement.getParent());
						} catch (JavaModelException e) {
							// ignore, we will not show the element
						}
					}
				}
			}
				
			/**
			 * Returns the progress monitor used to setup and report progress.
			 */
			public IProgressMonitor getProgressMonitor() {
				return pm;
			}
			
			public void aboutToStart() {
			}
			
			public void done() {
			}
		};
		
		search(JavaPlugin.getWorkspace(), "main(String[]) void", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, scope, collector); //$NON-NLS-1$
		return v;
	}

}