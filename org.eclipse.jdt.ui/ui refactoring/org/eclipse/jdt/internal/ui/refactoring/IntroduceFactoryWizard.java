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
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceFactoryRefactoring;

/**
 * @author rfuhrer@watson.ibm.com
 */
public class IntroduceFactoryWizard extends RefactoringWizard {
	/**
	 * Constructor for IntroduceFactoryWizard.
	 * @param ref
	 * @param pageTitle
	 * @param errorPageContextHelpId
	 */
	public IntroduceFactoryWizard(IntroduceFactoryRefactoring ref, String pageTitle) {
		super(ref, pageTitle);
		setExpandFirstNode(true);
	}

	/**
	 * @see RefactoringWizard#addUserInputPages
	 */
	protected void addUserInputPages() {
		String message= RefactoringMessages.getString("IntroduceFactoryInputPage.name_interface"); //$NON-NLS-1$

		IntroduceFactoryInputPage	page= new IntroduceFactoryInputPage(message);

		addPage(page);
	}

	public IntroduceFactoryRefactoring getIntroduceFactoryRefactoring() {
		return (IntroduceFactoryRefactoring) getRefactoring();
	}
}
