/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.core.manipulation.JavaElementLabelsCore;
import org.eclipse.jdt.internal.corext.CorextCore;
import org.eclipse.jdt.internal.corext.refactoring.Checks;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.base.RefactoringStatusCodes;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaStatusContext;
import org.eclipse.jdt.internal.corext.util.JdtFlags;
import org.eclipse.jdt.internal.corext.util.Messages;
import org.eclipse.jdt.internal.corext.util.MethodOverrideTester;

import org.eclipse.jdt.internal.ui.util.Progress;

public class MethodChecks {

	//no instances
	private MethodChecks(){
	}

	/**
	 * Returns <code>true</code> iff the method could be a virtual method,
	 * i.e. if it is not a constructor, is private, or is static.
	 *
	 * @param method a method
	 * @return <code>true</code> iff the method could a virtual method
	 */
	public static boolean isVirtual(IMethod method) throws JavaModelException {
		if (method.isConstructor())
			return false;
		if (JdtFlags.isPrivate(method))
			return false;
		if (JdtFlags.isStatic(method))
			return false;
		return true;
	}

	/**
	 * Returns <code>true</code> iff the method could be a virtual method,
	 * i.e. if it is not a constructor, is private, or is static.
	 *
	 * @param methodBinding a method
	 * @return <code>true</code> iff the method could a virtual method
	 */
	public static boolean isVirtual(IMethodBinding methodBinding){
		if (methodBinding.isConstructor())
			return false;
		if (Modifier.isPrivate(methodBinding.getModifiers()))
			return false;
		if (Modifier.isStatic(methodBinding.getModifiers()))
			return false;
		return true;
	}

	public static RefactoringStatus checkIfOverridesAnother(IMethod method, ITypeHierarchy hierarchy) throws JavaModelException {
		IMethod overrides= MethodChecks.overridesAnotherMethod(method, hierarchy);
		if (overrides == null)
			return null;

		RefactoringStatusContext context= JavaStatusContext.create(overrides);
		String message= Messages.format(RefactoringCoreMessages.MethodChecks_overrides,
				new String[]{JavaElementUtil.createMethodSignature(overrides), JavaElementLabelsCore.getElementLabel(overrides.getDeclaringType(), JavaElementLabelsCore.ALL_FULLY_QUALIFIED)});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, CorextCore.getPluginId(), RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD, overrides);
	}

	public static RefactoringStatus checkIfComesFromInterface(IMethod method, ITypeHierarchy hierarchy, IProgressMonitor monitor) throws JavaModelException {
		IMethod inInterface= MethodChecks.isDeclaredInInterface(method, hierarchy, monitor);

		if (inInterface == null)
			return null;

		RefactoringStatusContext context= JavaStatusContext.create(inInterface);
		String message= Messages.format(RefactoringCoreMessages.MethodChecks_implements,
				new String[]{JavaElementUtil.createMethodSignature(inInterface), JavaElementLabelsCore.getElementLabel(inInterface.getDeclaringType(), JavaElementLabelsCore.ALL_FULLY_QUALIFIED)});
		return RefactoringStatus.createStatus(RefactoringStatus.FATAL, message, context, CorextCore.getPluginId(), RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE, inInterface);
	}

	public static IMethod isDeclaredInInterface(IMethod method, ITypeHierarchy hierarchy, IProgressMonitor monitor) throws JavaModelException {
		Assert.isTrue(isVirtual(method));
		IProgressMonitor subMonitor= Progress.subMonitor(monitor, 1);
		try {
			IType[] classes= hierarchy.getAllClasses();
			subMonitor.beginTask("", classes.length); //$NON-NLS-1$
			for (IType clazz : classes) {
				IType[] superinterfaces= null;
				if (clazz.equals(hierarchy.getType()))
					superinterfaces= hierarchy.getAllSuperInterfaces(clazz);
				else
					superinterfaces= clazz.newSupertypeHierarchy(Progress.subMonitor(subMonitor, 1)).getAllSuperInterfaces(clazz);
				for (IType superinterface : superinterfaces) {
					IMethod found= Checks.findSimilarMethod(method, superinterface);
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
		MethodOverrideTester tester= new MethodOverrideTester(method.getDeclaringType(), hierarchy);
		IMethod found= tester.findDeclaringMethod(method, true);
		boolean overrides= (found != null && !found.equals(method) && (!JdtFlags.isStatic(found)) && (!JdtFlags.isPrivate(found)));
		if (overrides)
			return found;
		else
			return null;
	}

	/**
	 * Locates the topmost method of an override ripple and returns it. If none
	 * is found, null is returned.
	 *
	 * @param method the IMethod which may be part of a ripple
	 * @param typeHierarchy a ITypeHierarchy of the declaring type of the method. May be null
	 * @param monitor an IProgressMonitor
	 * @return the topmost method of the ripple, or null if none
	 */
	public static IMethod getTopmostMethod(IMethod method, ITypeHierarchy typeHierarchy, IProgressMonitor monitor) throws JavaModelException {

		Assert.isNotNull(method);

		ITypeHierarchy hierarchy= typeHierarchy;
		IMethod topmostMethod= null;
		final IType declaringType= method.getDeclaringType();
		if (!declaringType.isInterface()) {
			if ((hierarchy == null) || !declaringType.equals(hierarchy.getType()))
				hierarchy= declaringType.newTypeHierarchy(monitor);

			IMethod inInterface= isDeclaredInInterface(method, hierarchy, monitor);
			if (inInterface != null && !inInterface.equals(method))
				topmostMethod= inInterface;
		}
		if (topmostMethod == null) {
			if (hierarchy == null)
				hierarchy= declaringType.newSupertypeHierarchy(monitor);
			IMethod overrides= overridesAnotherMethod(method, hierarchy);
			if (overrides != null && !overrides.equals(method))
				topmostMethod= overrides;
		}
		return topmostMethod;
	}
}
