/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.scripting;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.internal.core.refactoring.history.RefactoringInstanceCreator;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.rename.JavaRenameRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.rename.MethodChecks;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameNonVirtualMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;

/**
 * Refactoring instance creator for the rename method refactoring.
 * 
 * @since 3.2
 */
public final class RenameMethodRefactoringInstanceCreator extends RefactoringInstanceCreator {

	/**
	 * {@inheritDoc}
	 */
	public Refactoring createRefactoring(final RefactoringDescriptor descriptor) throws JavaModelException {
		RenameProcessor processor= null;
		final String handle= (String) descriptor.getArguments().get(RenameMethodProcessor.ATTRIBUTE_HANDLE);
		if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
			final IJavaElement element= JavaCore.create(handle);
			if (element instanceof IMethod && element.exists()) {
				final IMethod method= (IMethod) element;
				if (MethodChecks.isVirtual(method))
					processor= new RenameVirtualMethodProcessor(null);
				else
					processor= new RenameNonVirtualMethodProcessor(null);
				return new JavaRenameRefactoring(processor);
			}
		}
		return null;
	}
}