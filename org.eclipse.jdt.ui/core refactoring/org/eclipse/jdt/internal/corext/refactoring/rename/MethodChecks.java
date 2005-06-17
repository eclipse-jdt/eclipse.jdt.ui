/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

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
import org.eclipse.jdt.internal.corext.util.Messages;

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
	
	public static RefactoringStatus checkIfOverridesAnother(IMethod method, ITypeHierarchy hierarchy) throws JavaModelException {
		IMethod overrides= MethodChecks.overridesAnotherMethod(method, hierarchy);
		if (overrides == null)
			return null;

		RefactoringStatusContext context= JavaStatusContext.create(overrides);
		String message= Messages.format(RefactoringCoreMessages.MethodChecks_overrides, 
				new String[]{JavaElementUtil.createMethodSignature(overrides), JavaModelUtil.getFullyQualifiedName(overrides.getDeclaringType())});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, Corext.getPluginId(), RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD, overrides);
	}
	
	/**
	 * Checks if the given method is declared in an interface. If the method's declaring type
	 * is an interface the method returns <code>false</code> if it is only declared in that
	 * interface.
	 */
	public static RefactoringStatus checkIfComesFromInterface(IMethod method, ITypeHierarchy hierarchy, IProgressMonitor monitor) throws JavaModelException {
		IMethod inInterface= MethodChecks.isDeclaredInInterface(method, hierarchy, monitor);
			
		if (inInterface == null)
			return null;

		RefactoringStatusContext context= JavaStatusContext.create(inInterface);
		String message= Messages.format(RefactoringCoreMessages.MethodChecks_implements, 
				new String[]{JavaElementUtil.createMethodSignature(inInterface), JavaModelUtil.getFullyQualifiedName(inInterface.getDeclaringType())});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, Corext.getPluginId(), RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE, inInterface);
	}
	
	/**
	 * Checks if the given method is declared in an interface. If the method's declaring type
	 * is an interface the method returns <code>false</code> if it is only declared in that
	 * interface.
	 */
	public static IMethod isDeclaredInInterface(IMethod method, ITypeHierarchy hierarchy, IProgressMonitor monitor) throws JavaModelException {
		Assert.isTrue(isVirtual(method));
		IProgressMonitor subMonitor= new SubProgressMonitor(monitor, 1);
		try {
			IType[] classes= hierarchy.getAllClasses();
			subMonitor.beginTask("", classes.length); //$NON-NLS-1$
			for (int i= 0; i < classes.length; i++) {
				final IType clazz= classes[i];
				IType[] superinterfaces= null;
				if (clazz.equals(hierarchy.getType()))
					superinterfaces= hierarchy.getAllSuperInterfaces(clazz);
				else
					superinterfaces= clazz.newSupertypeHierarchy(new SubProgressMonitor(subMonitor, 1)).getAllSuperInterfaces(clazz);
				for (int j= 0; j < superinterfaces.length; j++) {
					IMethod found= Checks.findSimilarMethod(method, superinterfaces[j]);
					if (found != null && !found.equals(method))
						return found;
				}
				subMonitor.worked(1);
			}
			return null;
		} finally {
			subMonitor.done();
		}
	}

	public static IMethod overridesAnotherMethod(IMethod method, ITypeHierarchy hierarchy) throws JavaModelException {
		IMethod found= JavaModelUtil.findMethodDeclarationInHierarchy(hierarchy, method.getDeclaringType(), method.getElementName(), method.getParameterTypes(), method.isConstructor());
		boolean overrides= (found != null && !found.equals(method) && (!JdtFlags.isStatic(found)) && (!JdtFlags.isPrivate(found)));
		if (overrides)
			return found;
		else
			return null;
	}
}
