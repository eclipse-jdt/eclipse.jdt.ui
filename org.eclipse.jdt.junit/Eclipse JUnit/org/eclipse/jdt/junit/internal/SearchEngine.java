/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Search engine that looks for the suite() methods
 */
public class SearchEngine extends org.eclipse.jdt.core.search.SearchEngine {

	private class JUnitSearchResultCollector implements IJavaSearchResultCollector {
		IProgressMonitor fProgressMonitor;
		List fList;
		
		public JUnitSearchResultCollector(List list, IProgressMonitor progressMonitor) {
			fProgressMonitor= progressMonitor;
			fList= list;
		}
		
		public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws JavaModelException{
			if (!(enclosingElement instanceof IMethod)) return;
			
			IMethod method= (IMethod)enclosingElement;		
			if (!SearchUtil.isTestType(method.getDeclaringType()) && !SearchUtil.hasSuiteMethod(method.getDeclaringType())) return;
												
				if (!fList.contains(enclosingElement.getParent()))
					fList.add(enclosingElement.getParent());
		}
		
		public IProgressMonitor getProgressMonitor() {
			return fProgressMonitor;
		}
				
		public void aboutToStart() {
		}
		
		public void done() {
		}
	}
	
	public List searchMethod(IRunnableContext context, final IJavaSearchScope scope) 
			throws InvocationTargetException {

		final List typesFound= new ArrayList(200);	
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) {
				try {
					searchMethod(typesFound, scope, pm);
				} catch (CoreException e) {;
					Status status= new Status(IStatus.ERROR, JUnitPlugin.getPluginID(), IStatus.OK, "", e);
					JUnitPlugin.getDefault().getLog().log(status);
					MessageDialog.openError(JUnitPlugin.getActiveShell(), "JUnit Launcher Error", e.toString());								
				}
			}
		};
		try {
			context.run(true, true, runnable);
		} catch (InterruptedException e) {
			// do nothing user canceled the search process
		}
		return typesFound;	
	}

	private List searchMethod(final List v, IJavaSearchScope scope, final IProgressMonitor progressMonitor) throws JavaModelException, CoreException {		
		IJavaSearchResultCollector collector= new JUnitSearchResultCollector(v, progressMonitor);
		ISearchPattern suitePattern= createSearchPattern("suite() Test", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, true); //$NON-NLS-1$
		ISearchPattern testPattern= createSearchPattern("test*() void", IJavaSearchConstants.METHOD , IJavaSearchConstants.DECLARATIONS, true); //$NON-NLS-1$
		search(ResourcesPlugin.getWorkspace(), createOrSearchPattern(suitePattern, testPattern), scope, collector); 

		return v;
	}

	public static List findTargets(IStructuredSelection selection) 
			throws InvocationTargetException {
				
		List v= new ArrayList(10);
		Iterator elements= selection.iterator();
		while (elements.hasNext()) {
			Object o= elements.next();
				SearchUtil.collectTypes(v, o);
		}
		return v;
	}
}