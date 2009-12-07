/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.refactoring.participants.ChangeMethodSignatureArguments;
import org.eclipse.jdt.core.refactoring.participants.ChangeMethodSignatureParticipant;
import org.eclipse.jdt.core.refactoring.participants.ChangeMethodSignatureArguments.Parameter;
import org.eclipse.jdt.core.refactoring.participants.ChangeMethodSignatureArguments.ThrownException;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.JdtFlags;

public class TestChangeMethodSignaturParticipant extends ChangeMethodSignatureParticipant {

	static TestChangeMethodSignaturParticipant fgInstance;

	public static void testParticipant(IType type) throws JavaModelException {
		Assert.assertNotNull(fgInstance);
		fgInstance.test(type);
	}

	private void test(IType type) throws JavaModelException {
		Assert.assertNotNull(fElement);
		Assert.assertNotNull(fArguments);

		JavaModelUtil.reconcile(type.getCompilationUnit());

		String name= fArguments.getNewName();
		Parameter[] newParameters= fArguments.getNewParameters();
		String[] parameterTypesSigs= new String[newParameters.length];
		for (int i= 0; i < parameterTypesSigs.length; i++) {
			parameterTypesSigs[i]= newParameters[i].getType();
		}

		IMethod newMethod= JavaModelUtil.findMethod(name, parameterTypesSigs, fIsConstructor, type);
		Assert.assertNotNull(newMethod);

		assertEqualSignature(newMethod.getReturnType(), fArguments.getNewReturnType());
		Assert.assertEquals(JdtFlags.getVisibilityCode(newMethod), fArguments.getNewVisibility());

		String[] parameterNames= newMethod.getParameterNames();
		for (int i= 0; i < newParameters.length; i++) {
			Assert.assertEquals(parameterNames[i], newParameters[i].getName());
		}

		ThrownException[] thrownExceptions= fArguments.getThrownExceptions();
		String[] exceptionTypes= newMethod.getExceptionTypes();
		Assert.assertEquals(exceptionTypes.length, thrownExceptions.length);

		for (int i= 0; i < exceptionTypes.length; i++) {
			assertEqualSignature(exceptionTypes[i], thrownExceptions[i].getType());
		}
	}

	private static void assertEqualSignature(String expected, String actual) {
		if (!expected.equals(actual)) {
			String t1= Signature.getSimpleName(Signature.toString(expected));
			String t2= Signature.getSimpleName(Signature.toString(actual));
			if (!t1.equals(t2)) {
				Assert.assertEquals(expected, actual);
			}
		}
	}

	private Object fElement;
	private boolean fIsConstructor;
	private ChangeMethodSignatureArguments fArguments;

	public TestChangeMethodSignaturParticipant() {
	}

	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context) throws OperationCanceledException {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	public String getName() {
		return getClass().getName();
	}

	protected boolean initialize(Object element) {
		fgInstance= this;
		fElement= element;
		fArguments= getArguments();
		try {
			fIsConstructor= ((IMethod) element).isConstructor();
		} catch (JavaModelException e) {
			e.printStackTrace();
		}

		return true;
	}

}
