/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;

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
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

public class MainMethodFinder {

	public List findTargets(IRunnableContext context, IStructuredSelection selection) {
		final ArrayList result= new ArrayList();
		
		final List elements= SelectionUtil.toList(selection);
		if (elements != null && !elements.isEmpty()) {
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor pm) {
					int nElements= elements.size();
					pm.beginTask(LauncherMessages.getString("MainMethodFinder.description"), nElements);
					try {
						for (int i= 0; i < nElements; i++) {
							try {
								collectTypes(elements.get(i), new SubProgressMonitor(pm, 1), result);
							} catch (JavaModelException e) {
								JavaPlugin.log(e.getStatus());
							}
						}
					} finally {
						pm.done();
					}
				}
			};
			try {
				context.run(true, true, runnable);
			} catch (InvocationTargetException e) {
				JavaPlugin.log(e);
			} catch (InterruptedException e) {
				// user pressed cancel
			}
				
		}
		return result;
	}
			
	private void collectTypes(Object element, IProgressMonitor monitor, List result) throws JavaModelException {
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
						return;
					} else if (openable.getElementType() == IJavaElement.CLASS_FILE) {
						IType mainType= ((IClassFile)openable).getType();
						if ( JavaModelUtil.hasMainMethod(mainType)) {
							result.add(parentType);
						}
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
			}
		}
	}
	
	private IType[] searchMainMethods(IResource res, IProgressMonitor monitor) throws JavaModelException {
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IResource[] { res });
		MainMethodSearchEngine searchEngine= new MainMethodSearchEngine();
		return searchEngine.searchMainMethods(monitor, scope, IJavaElementSearchConstants.CONSIDER_BINARIES);
	}
}
	