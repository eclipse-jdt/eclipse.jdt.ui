/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

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
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

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
public class TestSearchEngine extends SearchEngine {

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

	public static List findTargets(IStructuredSelection selection) throws InvocationTargetException {			
		List v= new ArrayList(10);
		Iterator elements= selection.iterator();
		while(elements.hasNext()) {
			Object o= elements.next();
				collectTypes(v, o);
		}
		return v;
	}
	
	protected static void collectTypes(List v, Object scope) throws InvocationTargetException {
		try {
			scope= computeScope(scope);
			while((scope instanceof IJavaElement) && !(scope instanceof ICompilationUnit) && (scope instanceof ISourceReference)) {
				if(scope instanceof IType) {
					if (hasSuiteMethod((IType)scope) || isTestType((IType)scope)) {
						v.add(scope);
						return;
					}
				}
				scope= ((IJavaElement)scope).getParent();
			}
			if (scope instanceof ICompilationUnit) {
				ICompilationUnit cu= (ICompilationUnit)scope;
				IType[] types= cu.getAllTypes();
				for (int i= 0; i < types.length; i++) {
					if (hasSuiteMethod(types[i])  || isTestType(types[i]))
						v.add(types[i]);
				}
			} 
			else if (scope instanceof IJavaElement) {
				List found= searchSuiteMethods(new ProgressMonitorDialog(JUnitPlugin.getActiveShell()), (IJavaElement)scope);
				v.addAll(found);
			}
		} catch (JavaModelException e) {
			throw new InvocationTargetException(e);
		}
	}

	protected static Object computeScope(Object scope) throws InvocationTargetException {
		if (scope instanceof IProcess) 
			scope= ((IProcess)scope).getLaunch();
		if (scope instanceof IDebugTarget)
			scope= ((IDebugTarget)scope).getLaunch();
		if (scope instanceof ILaunch) 
			scope= ((ILaunch)scope).getElement();
		if (scope instanceof IFileEditorInput)
			scope= ((IFileEditorInput)scope).getFile();
		if (scope instanceof IResource)
			scope= JavaCore.create((IResource)scope);
		if (scope instanceof IClassFile) {
			IClassFile cf= (IClassFile)scope;
			try {
				scope= cf.getType();
			} catch (JavaModelException e) {
				throw new InvocationTargetException(e);
			}
		}
		return scope;
	}
	
	private static List searchSuiteMethods(IRunnableContext context, IJavaElement element) 
			throws InvocationTargetException {	
		IJavaSearchScope scope= TestSearchEngine.createJavaSearchScope(new IJavaElement[] { element });
		return new TestSearchEngine().searchMethod(context, scope);
	}
	
	protected static boolean hasSuiteMethod(IType type){
		try{
			IMethod method= type.getMethod("suite", new String[0]);
			if (method == null || !method.exists()) 
				return false;
			
			if (!Flags.isStatic(method.getFlags()) ||	
				!Flags.isPublic(method.getFlags()) ||			
				Flags.isAbstract(method.getDeclaringType().getFlags()) ||
				!Flags.isPublic(method.getDeclaringType().getFlags())) 
				return false;
		} catch (JavaModelException e){
			String msg= "Warning: hasSuiteMethod(IType type) failed: " + type.getElementName();
			Status status= new Status(IStatus.WARNING, JUnitPlugin.getPluginID(), IStatus.OK, msg, e);
			JUnitPlugin.getDefault().getLog().log(status);
			return false;
		}
		return true;
	}
	
	protected static boolean isTestType(IType type){
		try{
			if (Flags.isAbstract(type.getFlags())) 
				return false;
			if (!Flags.isPublic(type.getFlags())) 
				return false;
			
			IType[] interfaces= type.newTypeHierarchy(type.getJavaProject(), null).getAllSuperInterfaces(type);
			for (int i=0; i < interfaces.length; i++)
				if(interfaces[i].getFullyQualifiedName().equals("junit.framework.Test"))
					return true;
		} catch (JavaModelException e) {
			String msg= "Warning: isTestType(IType type) failed: " + type.getElementName();
			Status status= new Status(IStatus.WARNING, JUnitPlugin.getPluginID(), IStatus.OK, msg, e);
			JUnitPlugin.getDefault().getLog().log(status);
		}				
		return false;
	}

}