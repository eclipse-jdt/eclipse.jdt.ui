/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.scripting;

import java.util.Map;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;

import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringContribution;
import org.eclipse.jdt.internal.corext.refactoring.JDTRefactoringDescriptor;
import org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;

/**
 * Refactoring contribution for the rename method refactoring.
 * 
 * @since 3.2
 */
public final class RenameMethodRefactoringContribution extends JDTRefactoringContribution {

	/**
	 * {@inheritDoc}
	 */
	public Refactoring createRefactoring(final RefactoringDescriptor descriptor) throws JavaModelException {
		String project= descriptor.getProject();
		Map arguments= ((JDTRefactoringDescriptor) descriptor).getArguments();
		String input= (String) arguments.get(JDTRefactoringDescriptor.ATTRIBUTE_INPUT);
		IMethod method= (IMethod) JDTRefactoringDescriptor.handleToElement(project, input);
		
		JavaRenameProcessor processor;
		if (MethodChecks.isVirtual(method)) {
			processor= new RenameVirtualMethodProcessor(method);
		} else {
			processor= new RenameNonVirtualMethodProcessor(method);
		}
		return new JavaRenameRefactoring(processor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public RefactoringDescriptor createDescriptor() {
		return new RenameJavaElementDescriptor(IJavaRefactorings.RENAME_METHOD);
	}
}
