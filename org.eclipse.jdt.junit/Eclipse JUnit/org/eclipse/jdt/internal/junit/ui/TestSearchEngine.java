/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.IFileEditorInput;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;

/**
 * Custom Search engine for suite() methods
 */
public class TestSearchEngine /*extends SearchEngine*/ {

	private class JUnitSearchResultCollector implements IJavaSearchResultCollector {
		IProgressMonitor fProgressMonitor;
		List fList;
		
		public JUnitSearchResultCollector(List list, IProgressMonitor progressMonitor) {
			fProgressMonitor= progressMonitor;
			fList= list;
		}
		
		public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws JavaModelException{
			if (!(enclosingElement instanceof IMethod)) 
				return;
			
			IMethod method= (IMethod)enclosingElement;		
			if (!isTestType(method.getDeclaringType()) && !hasSuiteMethod(method.getDeclaringType())) 
				return;
												
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
	
	private List searchMethod(IProgressMonitor pm, final IJavaSearchScope scope) throws JavaModelException {
		final List typesFound= new ArrayList(200);	
		searchMethod(typesFound, scope, pm);
		return typesFound;	
	}

	private List searchMethod(final List v, IJavaSearchScope scope, final IProgressMonitor progressMonitor) throws JavaModelException {		
		IJavaSearchResultCollector collector= new JUnitSearchResultCollector(v, progressMonitor);
		ISearchPattern suitePattern= SearchEngine.createSearchPattern("suite() Test", IJavaSearchConstants.METHOD, IJavaSearchConstants.DECLARATIONS, true); //$NON-NLS-1$
		ISearchPattern testPattern= SearchEngine.createSearchPattern("test*() void", IJavaSearchConstants.METHOD , IJavaSearchConstants.DECLARATIONS, true); //$NON-NLS-1$
		SearchEngine engine= new SearchEngine();
		engine.search(ResourcesPlugin.getWorkspace(), SearchEngine.createOrSearchPattern(suitePattern, testPattern), scope, collector); 
		return v;
	}
	
	public static IType[] findTargets(IRunnableContext context, final Object[] elements) throws InvocationTargetException, InterruptedException{
		final Set result= new HashSet();
	
		if (elements.length > 0) {
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InterruptedException {
					int nElements= elements.length;
					pm.beginTask("Searching suites...", nElements); 
					try {
						for (int i= 0; i < nElements; i++) {
							try {
								collectTypes(elements[i], new SubProgressMonitor(pm, 1), result);
							} catch (JavaModelException e) {
								JUnitPlugin.log(e.getStatus());
							}
							if (pm.isCanceled()) {
								throw new InterruptedException();
							}
						}
					} finally {
						pm.done();
					}
				}
			};
			context.run(true, true, runnable);			
		}
		return (IType[]) result.toArray(new IType[result.size()]) ;
	}

	private static void collectTypes(Object element, IProgressMonitor pm, Set result) throws JavaModelException/*, InvocationTargetException*/ {
		element= computeScope(element);
		while((element instanceof IJavaElement) && !(element instanceof ICompilationUnit) && (element instanceof ISourceReference)) {
			if(element instanceof IType) {
				if (hasSuiteMethod((IType)element) || isTestType((IType)element)) {
					result.add(element);
					return;
				}
			}
			element= ((IJavaElement)element).getParent();
		}
		if (element instanceof ICompilationUnit) {
			ICompilationUnit cu= (ICompilationUnit)element;
			IType[] types= cu.getAllTypes();
			for (int i= 0; i < types.length; i++) {
				if (hasSuiteMethod(types[i])  || isTestType(types[i]))
					result.add(types[i]);
			}
		} 
		else if (element instanceof IJavaElement) {
			List found= searchSuiteMethods(pm, (IJavaElement)element);
			result.addAll(found);
		}
	}

	private static Object computeScope(Object element) throws JavaModelException {
		if (element instanceof IProcess) 
			element= ((IProcess)element).getLaunch();
		if (element instanceof IDebugTarget)
			element= ((IDebugTarget)element).getLaunch();
		if (element instanceof ILaunch) 
			element= ((ILaunch)element).getElement();
		if (element instanceof IFileEditorInput)
			element= ((IFileEditorInput)element).getFile();
		if (element instanceof IResource)
			element= JavaCore.create((IResource)element);
		if (element instanceof IClassFile) {
			IClassFile cf= (IClassFile)element;
			element= cf.getType();
		}
		return element;
	}
	
	private static List searchSuiteMethods(IProgressMonitor pm, IJavaElement element) throws JavaModelException {	
		// for backward compatibility with 1.0 
		// use the deprecated createJavaSearchScope(IResource[] constructor)
		//IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { element });
		IResource resource= element.getCorrespondingResource();
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IResource[] { resource });
		TestSearchEngine searchEngine= new TestSearchEngine(); 
		return searchEngine.searchMethod(pm, scope);
	}
	
	private static boolean hasSuiteMethod(IType type) throws JavaModelException {
		IMethod method= type.getMethod("suite", new String[0]);
		if (method == null || !method.exists()) 
			return false;
		
		if (!Flags.isStatic(method.getFlags()) ||	
			!Flags.isPublic(method.getFlags()) ||			
			Flags.isAbstract(method.getDeclaringType().getFlags()) ||
			!Flags.isPublic(method.getDeclaringType().getFlags())) { 
			return false;
		}
		return true;
	}
	
	private static boolean isTestType(IType type) throws JavaModelException {
		if (Flags.isAbstract(type.getFlags())) 
			return false;
		if (!Flags.isPublic(type.getFlags())) 
			return false;
		
		IType[] interfaces= type.newTypeHierarchy(type.getJavaProject(), null).getAllSuperInterfaces(type);
		for (int i=0; i < interfaces.length; i++)
			if(interfaces[i].getFullyQualifiedName().equals("junit.framework.Test"))
				return true;
		return false;
	}
}