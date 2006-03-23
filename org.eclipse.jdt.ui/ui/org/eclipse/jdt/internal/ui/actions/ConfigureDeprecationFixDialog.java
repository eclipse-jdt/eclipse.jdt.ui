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
package org.eclipse.jdt.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.Window;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.IBinding;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.deprecation.CreateDeprecationFixChange;
import org.eclipse.jdt.internal.corext.refactoring.deprecation.DeprecationRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.DeleteFileChange;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;

/**
 * Dialog to configure a deprecation fix.
 * 
 * @since 3.2
 */
public final class ConfigureDeprecationFixDialog extends TrayDialog {

	private static final int INLINE_FIX= 1;

	private static final int NO_FIX= 0;

	/** The binding */
	private final IBinding fBinding;

	/** The current fix */
	private int fCurrentFix= -1;

	/** The future fix */
	private int fFutureFix= -1;

	/** The inline fix button */
	private Button fInlineFixButton;

	/** The no fix button */
	private Button fNoFixButton;

	/** The java project */
	private final IJavaProject fProject;

	/**
	 * Creates a new configure deprecation fix dialog.
	 * 
	 * @param shell
	 *            the parent shell
	 * @param binding
	 *            the binding
	 */
	public ConfigureDeprecationFixDialog(final Shell shell, final IJavaProject project, final IBinding binding) {
		super(shell);
		Assert.isNotNull(project);
		Assert.isNotNull(binding);
		fProject= project;
		fBinding= binding;
	}

	/**
	 * {@inheritDoc}
	 */
	protected void configureShell(final Shell shell) {
		super.configureShell(shell);
		shell.setText(ActionMessages.ConfigureDeprecationFixDialog_dialog_title);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.CONFIGURE_DEPRECATION_FIX_ACTION);
	}

	/**
	 * {@inheritDoc}
	 */
	protected Control createDialogArea(final Composite parent) {
		final Composite container= (Composite) super.createDialogArea(parent);

		final Composite composite= new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout());

		final Label label= new Label(composite, SWT.HORIZONTAL | SWT.LEFT | SWT.WRAP);
		label.setText(ActionMessages.ConfigureDeprecationFixDialog_dialog_description);

		fNoFixButton= new Button(composite, SWT.RADIO);
		fNoFixButton.setText(ActionMessages.ConfigureDeprecationFixDialog_no_fix_label);
		fNoFixButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				fFutureFix= NO_FIX;
			}
		});

		final GridData data= new GridData();
		data.verticalIndent= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING * 2);
		fNoFixButton.setLayoutData(data);

		fInlineFixButton= new Button(composite, SWT.RADIO);
		fInlineFixButton.setText(ActionMessages.ConfigureDeprecationFixDialog_inline_fix_label);
		fInlineFixButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent event) {
				fFutureFix= INLINE_FIX;
			}
		});

		initializeFix();
		initializeControls();

		return composite;
	}

	/**
	 * Initializes the controls of this dialog.
	 */
	private void initializeControls() {
		switch (fCurrentFix) {
			case NO_FIX:
				fNoFixButton.setSelection(true);
				break;
			case INLINE_FIX:
				fInlineFixButton.setSelection(true);
				break;
		}
	}

	/**
	 * Initializes the current fix.
	 */
	private void initializeFix() {
		final RefactoringHistory history= DeprecationRefactorings.getRefactoringHistory(fProject, fBinding);
		if (history != null) {
			final RefactoringDescriptorProxy[] proxies= history.getDescriptors();
			if (proxies.length == 1) {
				final RefactoringDescriptor descriptor= proxies[0].requestDescriptor(null);
				if (descriptor != null && descriptor.getID().equals(InlineMethodRefactoring.ID_INLINE_METHOD))
					fCurrentFix= INLINE_FIX;
			}
		} else
			fCurrentFix= NO_FIX;
		fFutureFix= fCurrentFix;
	}

	/**
	 * {@inheritDoc}
	 */
	public int open() {
		final int result= super.open();
		if (result == Window.OK && fCurrentFix != fFutureFix) {
			Change change= null;
			final IFile file= DeprecationRefactorings.getRefactoringScriptFile(fProject, DeprecationRefactorings.getRefactoringScriptName(fBinding));
			if (fFutureFix == NO_FIX) {
				change= new DeleteFileChange(file);
			} else if (fFutureFix == INLINE_FIX) {
				change= new CreateDeprecationFixChange(file.getFullPath(), DeprecationRefactorings.createInlineDeprecationScript(fBinding), CorrectionMessages.QuickAssistProcessor_create_fix_name);
			}
			if (change != null) {
				try {
					change.initializeValidationData(new NullProgressMonitor());
					final WorkbenchRunnableAdapter adapter= new WorkbenchRunnableAdapter(RefactoringUI.createUIAwareChangeOperation(change));
					PlatformUI.getWorkbench().getProgressService().runInUI(new BusyIndicatorRunnableContext(), adapter, ResourcesPlugin.getWorkspace().getRoot());
				} catch (InvocationTargetException exception) {
					JavaPlugin.log(exception);
				} catch (InterruptedException exception) {
					// Do nothing
				} finally {
					change.dispose();
				}
			}
		}
		return result;
	}
}