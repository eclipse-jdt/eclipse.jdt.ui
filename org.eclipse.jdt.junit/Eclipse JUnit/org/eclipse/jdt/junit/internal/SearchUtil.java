/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IFileEditorInput;


public class SearchUtil {
	
	private SearchUtil() {
	}
	
	protected static void collectTypes(List v, Object scope)
			throws InvocationTargetException {

		try {
			scope = computeScope(scope);
			while ((scope instanceof IJavaElement) && !(scope instanceof ICompilationUnit) && (scope instanceof ISourceReference)) {
				if (scope instanceof IType) {
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
				IResource res= ((IJavaElement)scope).getUnderlyingResource();
				if (res != null) {
					IProject p= res.getProject();
					List found= searchSuiteMethods(new ProgressMonitorDialog(JUnitPlugin.getActiveShell()), res);
					v.addAll(found);
				}
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
	
	private static List searchSuiteMethods(IRunnableContext context, IResource res) 
			throws InvocationTargetException {	
		IJavaSearchScope scope= SearchEngine.createJavaSearchScope(new IResource[] { res });
		SearchEngine searchEngine= new SearchEngine();
		return searchEngine.searchMethod(context, scope);
	}
	
	protected static boolean hasSuiteMethod(IType type){
		try{
			IMethod method= type.getMethod("suite", new String[0]);
			if (method == null || !method.exists()) return false;
			
			if (!Flags.isStatic(method.getFlags()) ||	
				!Flags.isPublic(method.getFlags()) ||			
				Flags.isAbstract(method.getDeclaringType().getFlags()) ||
				!Flags.isPublic(method.getDeclaringType().getFlags()) 
			) return false;
		} catch (JavaModelException e){
			String msg= "warning: hasSuiteMethod(IType type) failed: " + type.getElementName();
			Status status= new Status(IStatus.WARNING, JUnitPlugin.getPluginID(), IStatus.OK, msg, e);
			JUnitPlugin.getDefault().getLog().log(status);
			return false;
		}
		return true;
	}
	
	protected static boolean isTestType(IType type){
		try{
			if (Flags.isAbstract(type.getFlags())) return false;
			if (!Flags.isPublic(type.getFlags())) return false;
			
			IType[] interfaces= type.newTypeHierarchy(type.getJavaProject(), null).getAllSuperInterfaces(type);
			for (int i=0; i < interfaces.length; i++)
				if(interfaces[i].getFullyQualifiedName().equals("junit.framework.Test"))
					return true;
		} catch (JavaModelException e) {
			String msg= "warning: isTestType(IType type) failed: " + type.getElementName();
			Status status= new Status(IStatus.WARNING, JUnitPlugin.getPluginID(), IStatus.OK, msg, e);
			JUnitPlugin.getDefault().getLog().log(status);
		}				
		return false;
	}
}

