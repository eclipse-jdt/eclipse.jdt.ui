/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sebastian Davids <sdavids@gmx.de> bug 38692
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import org.eclipse.jdt.internal.ui.javadocexport.JavadocWizard;
import org.eclipse.jdt.internal.ui.preferences.JavadocPreferencePage;

public class GenerateJavadocAction implements IWorkbenchWindowActionDelegate {

	private ISelection fSelection;
	private IWorkbenchWindow fWindow;

	//bug 38692
	class JavadocSetCommandDialog extends Dialog {

		private JavadocPreferencePage fPage;

		/**
		 * Creates a dialog instance.
		 *
		 * @param parentShell the parent shell, or <code>null</code> to create a top-level shell
		 */
		public JavadocSetCommandDialog(Shell parentShell) {
			super(parentShell);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#configureShell(org.eclipse.swt.widgets.Shell)
		 */
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(ActionMessages.getString("GenerateJavadoc.setjavadoccommanddialog.title")); //$NON-NLS-1$
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
		 */
		protected Control createDialogArea(Composite parent) {
			Composite composite= (Composite) super.createDialogArea(parent);
			fPage= new JavadocPreferencePage() {
				public void createControl(Composite parentComposite) {
					noDefaultAndApplyButton();
					super.createControl(parentComposite);
				}

				protected void updateStatus(IStatus status) {
					setValid(!(status.matches(IStatus.ERROR) || status.matches(IStatus.INFO)));
					Button okButton= getButton(IDialogConstants.OK_ID);

					if (okButton != null)
						okButton.setEnabled(isValid());
				}
			};
			fPage.createControl(composite);
			Dialog.applyDialogFont(composite);
			return composite;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
		 */
		protected void okPressed() {
			fPage.performOk();
			super.okPressed();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
		 */
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}
	}
	
	public void dispose() {
	}

	public void init(IWorkbenchWindow window) {
		fWindow= window;
	}

	public void run(IAction action) {
		//bug 38692
		if (JavadocPreferencePage.getJavaDocCommand().length() == 0) {
			Dialog dialog= new JavadocSetCommandDialog(fWindow.getShell());
			dialog.open();
			if (dialog.getReturnCode() == Dialog.CANCEL)
				return;
		}
		JavadocWizard wizard= new JavadocWizard();
		IStructuredSelection selection= null;
		if (fSelection instanceof IStructuredSelection) {
			selection= (IStructuredSelection)fSelection;
		} else {
			selection= new StructuredSelection();
		}
		
		wizard.init(fWindow.getWorkbench() , selection);
		WizardDialog dialog= new WizardDialog(fWindow.getShell(), wizard);
		dialog.open();
	}

	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
	}
}
