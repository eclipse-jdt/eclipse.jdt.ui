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

import org.eclipse.jdt.internal.corext.refactoring.structure.MoveStaticMembersRefactoring;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class MoveMembersWizard extends RefactoringWizard {

	public MoveMembersWizard(MoveStaticMembersRefactoring ref) {
		this(
			ref, 
			RefactoringMessages.getString("RefactoringGroup.move_Members"), //$NON-NLS-1$
			IJavaHelpContextIds.MOVE_MEMBERS_ERROR_WIZARD_PAGE
		);
	}

	public MoveMembersWizard(MoveStaticMembersRefactoring ref, String pageTitle, String errorPageContextHelpId) {
		super(ref, pageTitle, errorPageContextHelpId);
	}

	/* non java-doc
	 * @see RefactoringWizard#addUserInputPages
	 */ 
	protected void addUserInputPages(){
		setPageTitle(RefactoringMessages.getString("MoveMembersWizard.page_title")); //$NON-NLS-1$
		addPage(new MoveMembersInputPage());
	}
}
