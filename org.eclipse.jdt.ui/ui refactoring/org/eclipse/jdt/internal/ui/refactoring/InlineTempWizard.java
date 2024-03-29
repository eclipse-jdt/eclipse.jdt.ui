/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

import org.eclipse.swt.widgets.Composite;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.ui.refactoring.RefactoringWizard;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.refactoring.code.InlineTempRefactoring;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;

public class InlineTempWizard extends RefactoringWizard {

	public InlineTempWizard(InlineTempRefactoring ref) {
		super(ref, DIALOG_BASED_USER_INTERFACE | PREVIEW_EXPAND_FIRST_NODE | NO_BACK_BUTTON_ON_STATUS_DIALOG);
		setDefaultPageTitle(RefactoringMessages.InlineTempWizard_defaultPageTitle);
	}

	@Override
	protected void addUserInputPages() {
		addPage(new InlineTempInputPage());
	}

	@Override
	public int getMessageLineWidthInChars() {
		return 0;
	}

	private static class InlineTempInputPage extends MessageWizardPage {

		public static final String PAGE_NAME= "InlineTempInputPage"; //$NON-NLS-1$

		public InlineTempInputPage() {
			super(PAGE_NAME, MessageWizardPage.STYLE_QUESTION);
		}

		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IJavaHelpContextIds.INLINE_TEMP_WIZARD_PAGE);
		}

		@Override
		protected String getMessageString() {
			InlineTempRefactoring refactoring= (InlineTempRefactoring) getRefactoring();
			int occurrences= refactoring.getReferences().length;
			final String identifier= BasicElementLabels.getJavaElementName(refactoring.getVariableDeclaration().getName().getIdentifier());
			switch (occurrences) {
				case 0:
					return Messages.format(
							RefactoringMessages.InlineTempInputPage_message_zero,
							identifier);

				case 1:
					return Messages.format(RefactoringMessages.InlineTempInputPage_message_one, identifier);

				default:
					return Messages.format(RefactoringMessages.InlineTempInputPage_message_multi, new Object[] {
							Integer.valueOf(occurrences), identifier });
			}
		}
	}
}
