/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.JavaModelUtil;
import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

public class MainMethodFinder {
	
	private MainMethodFinder() {
	}

	public static IType[] findTargets(IRunnableContext context, final Object[] elements) throws InvocationTargetException, InterruptedException{
		final Set result= new HashSet();
	
		if (elements.length > 0) {
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) throws InterruptedException {
					int nElements= elements.length;
					pm.beginTask(LauncherMessages.getString("MainMethodFinder.description"), nElements);
					try {
						for (int i= 0; i < nElements; i++) {
							try {
								collectTypes(elements[i], new SubProgressMonitor(pm, 1), result);
							} catch (JavaModelException e) {
								JavaPlugin.log(e.getStatus());
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
			
	private static void collectTypes(Object element, IProgressMonitor monitor, Set result) throws JavaModelException {
		if (element instanceof IProcess) {
			element= ((IProcess)element).getLaunch();
		} else if (element instanceof IDebugTarget) {
			element= ((IDebugTarget)element).getLaunch();
		}
		
		if (element instanceof ILaunch) 
			element= ((ILaunch)element).getElement();

		if (element instanceof IAdaptable) {
			IResource resource= null;
			IJavaElement jelem= (IJavaElement) ((IAdaptable) element).getAdapter(IJavaElement.class);
			if (jelem != null) {
				IType parentType= (IType) JavaModelUtil.findElementOfKind(jelem, IJavaElement.TYPE);
				if (parentType != null && JavaModelUtil.hasMainMethod((IType) parentType)) {
					result.add(parentType);
					monitor.done();
					return;
				}
				IJavaElement openable= (IJavaElement) JavaModelUtil.getOpenable(jelem);
				if (openable != null) {
					if (openable.getElementType() == IJavaElement.COMPILATION_UNIT) {
						ICompilationUnit cu= (ICompilationUnit) openable;
						IType mainType= cu.getType(Signature.getQualifier(cu.getElementName()));
						if (mainType.exists() && JavaModelUtil.hasMainMethod(mainType)) {
							result.add(mainType);
						}
						monitor.done();
						return;
					} else if (openable.getElementType() == IJavaElement.CLASS_FILE) {
						IType mainType= ((IClassFile)openable).getType();
						if (JavaModelUtil.hasMainMethod(mainType)) {
							result.add(mainType);
						}
						monitor.done();
						return;	
					}
					resource= openable.getUnderlyingResource();
				}
			}
			if (resource == null) {
				resource= (IResource) ((IAdaptable) element).getAdapter(IResource.class);
			}
			if (resource != null) {
				IType[] types= searchMainMethods(resource, monitor);
				for (int i= 0; i < types.length; i++) {
					result.add(types[i]);
				}
			} else {
				monitor.done();
			}
		}
	}
	
	private static IType[] searchMainMethods(IResource res, IProgressMonitor monitor) throws JavaModelException {
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IResource[] { res });
		MainMethodSearchEngine searchEngine= new MainMethodSearchEngine();
		return searchEngine.searchMainMethods(monitor, scope, IJavaElementSearchConstants.CONSIDER_BINARIES);
	}
}
	