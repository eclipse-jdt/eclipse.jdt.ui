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
package org.eclipse.jdt.internal.corext.refactoring.rename;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.internal.core.refactoring.history.IInitializableRefactoringObject;
import org.eclipse.osgi.util.NLS;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;

/**
 * A rename refactoring which can be initialized with refactoring arguments.
 * 
 * @since 3.2
 */
public class JavaRenameRefactoring extends RenameRefactoring implements IInitializableRefactoringObject {

	/**
	 * Creates a new java rename refactoring.
	 * 
	 * @param processor
	 *            the rename processor to use
	 */
	public JavaRenameRefactoring(final RenameProcessor processor) {
		super(processor);
	}

	/*
	 * @see org.eclipse.ltk.internal.core.refactoring.history.IInitializableRefactoringObject#initialize(org.eclipse.ltk.core.refactoring.participants.RefactoringArguments)
	 */
	public final RefactoringStatus initialize(final RefactoringArguments arguments) {
		Assert.isNotNull(arguments);
		final RefactoringProcessor processor= getProcessor();
		if (processor instanceof IInitializableRefactoringObject) {
			return ((IInitializableRefactoringObject) processor).initialize(arguments);
		}
		return RefactoringStatus.createFatalErrorStatus(NLS.bind(RefactoringCoreMessages.JavaRenameRefactoring_error_unsupported_initialization, getProcessor().getIdentifier()));
	}
}