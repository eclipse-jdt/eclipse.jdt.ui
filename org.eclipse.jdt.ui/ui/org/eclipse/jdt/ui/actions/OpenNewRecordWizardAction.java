/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

package org.eclipse.jdt.ui.actions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.INewWizard;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.ui.wizards.NewRecordWizardPage;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.actions.ActionMessages;
import org.eclipse.jdt.internal.ui.wizards.NewRecordCreationWizard;

/**
* <p>Action that opens the new enum wizard. The action initialized the wizard with either the selection
 * as configured by {@link #setSelection(IStructuredSelection)} or takes a preconfigured
 * new enum wizard page, see {@link #setConfiguredWizardPage(NewRecordWizardPage)}.
 * </p>
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noreference This class is not intended to be referenced by clients.
 */
public class OpenNewRecordWizardAction extends AbstractOpenWizardAction {

	private NewRecordWizardPage fPage;
	private boolean fOpenEditorOnFinish;

	/**
	 * Creates an instance of the <code>OpenNewRecordWizardAction</code>.
	 */
	public OpenNewRecordWizardAction() {
		setText(ActionMessages.OpenNewRecordWizardAction_text);
		setDescription(ActionMessages.OpenNewRecordWizardAction_description);
		setToolTipText(ActionMessages.OpenNewRecordWizardAction_tooltip);
		setImageDescriptor(JavaPluginImages.DESC_WIZBAN_NEWRECORD);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.OPEN_RECORD_WIZARD_ACTION);

		fPage= null;
		fOpenEditorOnFinish= true;
	}

	/**
	 * Sets a page to be used by the wizard or <code>null</code> to use a page initialized with values
	 * from the current selection (see {@link #getSelection()} and {@link #setSelection(IStructuredSelection)}).
	 * @param page the page to use or <code>null</code>
	 */
	public void setConfiguredWizardPage(NewRecordWizardPage page) {
		fPage= page;
	}

	/**
	 * Specifies if the wizard will open the created type with the default editor. The default behaviour is to open
	 * an editor.
	 *
	 * @param openEditorOnFinish if set, the wizard will open the created type with the default editor
	 *
	 * @since 3.3
	 */
	public void setOpenEditorOnFinish(boolean openEditorOnFinish) {
		fOpenEditorOnFinish= openEditorOnFinish;
	}

	@Override
	protected final INewWizard createWizard() throws CoreException {
		return new NewRecordCreationWizard(fPage, fOpenEditorOnFinish);
	}
}
