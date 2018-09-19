/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.corext.refactoring.structure.PullUpRefactoringProcessor;

import org.eclipse.jdt.internal.ui.JavaPluginImages;

/**
 * Refactoring wizard for the pull up refactoring.
 */
public final class PullUpWizard extends RefactoringWizard {

	/** The page name */
	private static final String PAGE_NAME= "PullUpMemberPage"; //$NON-NLS-1$

	private final PullUpRefactoringProcessor fProcessor;

	/**
	 * Creates a new pull up wizard.
	 *
	 * @param processor
	 *           the processor
	 *
	 * @param refactoring
	 *            the pull up refactoring
	 */
	public PullUpWizard(PullUpRefactoringProcessor processor, Refactoring refactoring) {
		super(refactoring, WIZARD_BASED_USER_INTERFACE);
		fProcessor= processor;
		setDefaultPageTitle(RefactoringMessages.PullUpWizard_defaultPageTitle);
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR_PULL_UP);
	}

	@Override
	protected void addUserInputPages() {
		final PullUpMethodPage page= new PullUpMethodPage(fProcessor);
		addPage(new PullUpMemberPage(PullUpWizard.PAGE_NAME, page, fProcessor));
		addPage(page);
	}
}
