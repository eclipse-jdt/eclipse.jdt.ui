/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.ui.IFileEditorInput;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ClassFileEditorInput;
import org.eclipse.jdt.internal.ui.util.JavaModelUtility;
import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;

public class MainMethodFinder implements IJavaSearchConstants {

	public List findTargets(IStructuredSelection selection) {
		List v= new ArrayList(10);
		Iterator elements= selection.iterator();
		try {
			while (elements.hasNext()) {
				Object o= elements.next();
					collectTypes(v, o);
			}
		} catch (CoreException e) {
			JavaLaunchUtils.errorDialog(JavaPlugin.getActiveWorkbenchShell(), LauncherMessages.getString("mainMethodFinder.error.title"), LauncherMessages.getString("mainMethodFinder.error.exception"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return v;
	}
	
	private void collectTypes(List v, Object scope) throws CoreException {
		if (scope instanceof IProcess) 
			scope= ((IProcess)scope).getLaunch();
		
		if (scope instanceof IDebugTarget)
			scope= ((IDebugTarget)scope).getLaunch();
		
		if (scope instanceof ILaunch) 
			scope= ((ILaunch)scope).getElement();
		
		if (scope instanceof IFileEditorInput) {
			scope= ((IFileEditorInput)scope).getFile();
		} else if (scope instanceof ClassFileEditorInput) {
			scope= ((ClassFileEditorInput)scope).getClassFile();
		}
		if (scope instanceof IResource) {
			scope= JavaCore.create((IResource)scope);
		}
		if (scope instanceof IClassFile) {
			IClassFile cf= (IClassFile)scope;
			scope= cf.getType();
		}
		while ((scope instanceof IJavaElement) && !(scope instanceof ICompilationUnit) && (scope instanceof ISourceReference)) {
			if (scope instanceof IType) {
				if (JavaModelUtility.hasMainMethod((IType)scope)) {
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
				if (JavaModelUtility.hasMainMethod(types[i]))
					v.add(types[i]);
			}
		} else if (scope instanceof IJavaElement) {
			IResource res= ((IJavaElement)scope).getUnderlyingResource();
			if (res != null) {
				IProject p= res.getProject();
				List found= searchMainMethods(new ProgressMonitorDialog(JavaPlugin.getActiveWorkbenchShell()), res);
				v.addAll(found);
			}
		}
	}
	
	
	
	private List searchMainMethods(IRunnableContext context, IResource res) throws CoreException {
				
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IResource[] { res });
		MainMethodSearchEngine searchEngine= new MainMethodSearchEngine();
		return searchEngine.searchMethod(context, scope, IJavaElementSearchConstants.CONSIDER_BINARIES);
	}
	
}