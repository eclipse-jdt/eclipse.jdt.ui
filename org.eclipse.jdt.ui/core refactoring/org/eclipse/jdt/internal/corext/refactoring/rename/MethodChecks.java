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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.Corext;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.JavaStatusContext;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

public class MethodChecks {

	//no instances
	private MethodChecks(){
	}
	
	public static boolean isVirtual(IMethod method) throws JavaModelException {
		if (method.isConstructor())
			return false;
		if (JdtFlags.isPrivate(method))	
			return false;
		if (JdtFlags.isStatic(method))	
			return false;
		return true;	
	}	
	
	public static boolean isVirtual(IMethodBinding methodBinding){
		if (methodBinding.isConstructor())
			return false;
		if (Modifier.isPrivate(methodBinding.getModifiers()))	//TODO is this enough?
			return false;
		if (Modifier.isStatic(methodBinding.getModifiers()))	//TODO is this enough?
			return false;
		return true;	
	}
	
	public static RefactoringStatus checkIfOverridesAnother(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod overrides= MethodChecks.overridesAnotherMethod(method, pm);
		if (overrides == null)
			return null;

		RefactoringStatusContext context= JavaStatusContext.create(overrides);
		String message= RefactoringCoreMessages.getFormattedString("MethodChecks.overrides", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature(overrides), JavaModelUtil.getFullyQualifiedName(overrides.getDeclaringType())});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, Corext.getPluginId(), RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD, overrides);
	}
	
	/**
	 * Checks if the given method is declared in an interface. If the method's declaring type
	 * is an interface the method returns <code>false</code> if it is only declared in that
	 * interface.
	 */
	public static RefactoringStatus checkIfComesFromInterface(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod inInterface= MethodChecks.isDeclaredInInterface(method, pm);
			
		if (inInterface == null)
			return null;

		RefactoringStatusContext context= JavaStatusContext.create(inInterface);
		String message= RefactoringCoreMessages.getFormattedString("MethodChecks.implements", //$NON-NLS-1$
				new String[]{JavaElementUtil.createMethodSignature(inInterface), JavaModelUtil.getFullyQualifiedName(inInterface.getDeclaringType())});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, Corext.getPluginId(), RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE, inInterface);
	}
	
	/**
	 * Checks if the given method is declared in an interface. If the method's declaring type
	 * is an interface the method returns <code>false</code> if it is only declared in that
	 * interface.
	 */
	public static IMethod isDeclaredInInterface(IMethod method, IProgressMonitor pm) throws JavaModelException {
		Assert.isTrue(isVirtual(method));
		try{	
			pm.beginTask("", 4); //$NON-NLS-1$
			ITypeHierarchy hier= method.getDeclaringType().newTypeHierarchy(new SubProgressMonitor(pm, 1));
			IType[] classes= hier.getAllClasses();
			IProgressMonitor subPm= new SubProgressMonitor(pm, 3);
			subPm.beginTask("", classes.length); //$NON-NLS-1$
			for (int i= 0; i < classes.length; i++) {
				ITypeHierarchy superTypes= classes[i].newSupertypeHierarchy(new SubProgressMonitor(subPm, 1));
				IType[] superinterfaces= superTypes.getAllSuperInterfaces(classes[i]);
				for (int j= 0; j < superinterfaces.length; j++) {
					IMethod found= Checks.findSimilarMethod(method, superinterfaces[j]);
					if (found != null && !found.equals(method))
						return found;
				}
				subPm.worked(1);
			}
			return null;
		} finally{
			pm.done();
		}
	}
	
	public static IMethod overridesAnotherMethod(IMethod method, IProgressMonitor pm) throws JavaModelException {
		IMethod found= JavaModelUtil.findMethodDeclarationInHierarchy(
						method.getDeclaringType().newSupertypeHierarchy(pm), 
						method.getDeclaringType(), 
						method.getElementName(), 
						method.getParameterTypes(), 
						method.isConstructor());
		
		boolean overrides= (found != null && !found.equals(method) && (! JdtFlags.isStatic(found)) && (! JdtFlags.isPrivate(found)));	
		if (overrides)
			return found;
		else
			return null;	
	}
	
}
