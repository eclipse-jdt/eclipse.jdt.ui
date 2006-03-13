/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;

import org.eclipse.jdt.core.IMember;

import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;

/**
 * Refactoring processor for the extract supertype refactoring.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeProcessor extends PullUpRefactoringProcessor {

	private static final String ID_EXTRACT_SUPERTYPE= "org.eclipse.jdt.ui.extract.supertype"; //$NON-NLS-1$

	/**
	 * Creates a new extract supertype refactoring processor.
	 * 
	 * @param members
	 *            the members to extract, or <code>null</code> if invoked by scripting
	 * @param settings
	 *            the code generation settings, or <code>null</code> if invoked by scripting
	 */
	public ExtractSupertypeProcessor(final IMember[] members, final CodeGenerationSettings settings) {
		super(members, settings);
	}

	/**
	 * {@inheritDoc}
	 */
	public Change createChange(final IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		return super.createChange(monitor);
	}

	/**
	 * {@inheritDoc}
	 */
	public RefactoringStatus initialize(final RefactoringArguments arguments) {
		return super.initialize(arguments);
	}
}