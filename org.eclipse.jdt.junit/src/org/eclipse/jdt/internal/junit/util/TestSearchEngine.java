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
package org.eclipse.jdt.internal.junit.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.ISearchPattern;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;

/**
 * Custom Search engine for suite() methods
 */
public class TestSearchEngine {

	private class JUnitSearchResultCollector implements IJavaSearchResultCollector {
		IProgressMonitor fProgressMonitor;
		List fList;
		Set fFailed= new HashSet();
		Set fMatches= new HashSet();
		
		public JUnitSearchResultCollector(List list, IProgressMonitor progressMonitor) {
			fProgressMonitor= progressMonitor;
			fList= list;
		}
		
		public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) throws JavaModelException{
			if (!(enclosingElement instanceof IMethod)) 
				return;
			
			IMethod method= (IMethod)enclosingElement;		
			
			IType declaringType= method.getDeclaringType();
			if (fMatches.contains(declaringType) || fFailed.contains(declaringType))
				return;
			if (!hasSuiteMethod(declaringType) && !isTestType(declaringType)) {
				fFailed.add(declaringType);
				return;
			}
			fMatches.add(declaringType);
		}
		
		public IProgressMonitor getProgressMonitor() {
			return fProgressMonitor;
		}
				
		public void aboutToStart() {
		}
		
		public void done() {
			fList.addAll(fMatches);
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
	
	public static IType[] findTests(final Object[] elements) throws InvocationTargetException, InterruptedException{
		final Set result= new HashSet();
	
		if (elements.length > 0) {
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InterruptedException {
					doFindTests(elements, result, pm);
				}
			};
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);			
		}
		return (IType[]) result.toArray(new IType[result.size()]) ;
	}

	public static void doFindTests(Object[] elements, Set result, IProgressMonitor pm) throws InterruptedException {
		int nElements= elements.length;
		pm.beginTask(JUnitMessages.getString("TestSearchEngine.message.searching"), nElements);  //$NON-NLS-1$
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
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IJavaElement[] { element });
		TestSearchEngine searchEngine= new TestSearchEngine(); 
		return searchEngine.searchMethod(pm, scope);
	}
		
	public static boolean hasSuiteMethod(IType type) throws JavaModelException {
		IMethod method= type.getMethod("suite", new String[0]); //$NON-NLS-1$
		if (method == null || !method.exists()) 
			return false;
		
		if (!Flags.isStatic(method.getFlags()) ||	
			!Flags.isPublic(method.getFlags()) ||			
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
		
		IType[] interfaces= type.newSupertypeHierarchy(null).getAllSuperInterfaces(type);
		for (int i= 0; i < interfaces.length; i++)
			if(interfaces[i].getFullyQualifiedName().equals(JUnitPlugin.TEST_INTERFACE_NAME))
				return true;
		return false;
	}

	public static boolean isTestImplementor(IType type) throws JavaModelException {
		ITypeHierarchy typeHier= type.newSupertypeHierarchy(null);
		IType[] superInterfaces= typeHier.getAllInterfaces();
		for (int i= 0; i < superInterfaces.length; i++) {
			if (superInterfaces[i].getFullyQualifiedName().equals(JUnitPlugin.TEST_INTERFACE_NAME))
				return true;
		}
		return false;
	}

	public static boolean isTestOrTestSuite(IType type) throws JavaModelException {
		return hasSuiteMethod(type) || isTestType(type);
	}
}
