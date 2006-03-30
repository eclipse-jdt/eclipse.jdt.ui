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
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.Document;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.refactoring.code.InlineMethodRefactoring;
import org.eclipse.jdt.internal.corext.refactoring.deprecation.CreateDeprecationFixChange;
import org.eclipse.jdt.internal.corext.refactoring.deprecation.DeprecationRefactorings;
import org.eclipse.jdt.internal.corext.refactoring.nls.changes.DeleteFileChange;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jdt.internal.ui.text.correction.CorrectionMessages;
import org.eclipse.jdt.internal.ui.util.BusyIndicatorRunnableContext;
import org.eclipse.jdt.internal.ui.util.PixelConverter;

/**
 * Dialog to configure a deprecation fix.
 * 
 * @since 3.2
 */
public final class ConfigureDeprecationFixDialog extends StatusDialog {

	/** The replace invocation fix constant */
	private static final int REPLACE_FIX= 2;

	/** The inline fix constant */
	private static final int INLINE_FIX= 1;

	/** The no fix constant */
	private static final int NO_FIX= 0;

	/** The binding */
	private final IBinding fBinding;

	/** The current fix */
	private int fCurrentFix= NO_FIX;

	/** The future fix */
	private int fFutureFix= -1;

	/** The inline fix button */
	private Button fInlineFixButton;

	/** The no fix button */
	private Button fNoFixButton;

	/** The replace invocation fix button */
	private Button fReplaceInvocationButton;

	/** The java project */
	private final IJavaProject fProject;

	private Control fBodyEditorControl;

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
		final GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		composite.setLayout(layout);

		PixelConverter pixelConverter= new PixelConverter(composite);
		Label description= new Label(composite, SWT.HORIZONTAL | SWT.LEFT | SWT.WRAP);
		description.setText(ActionMessages.ConfigureDeprecationFixDialog_Description);
		GridData data= new GridData();
		data.widthHint= pixelConverter.convertWidthInCharsToPixels(90);
		description.setLayoutData(data);

		String memberName= fBinding.getName();


		if (canInlineMember()) {
			fNoFixButton= new Button(composite, SWT.CHECK);
			fNoFixButton.setText(Messages.format(ActionMessages.ConfigureDeprecationFixDialog_NoFixCheckBoxLabel, memberName));
			data= new GridData();
			data.verticalIndent= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING * 2);
			fNoFixButton.setLayoutData(data);

			fInlineFixButton= new Button(composite, SWT.RADIO);
			fInlineFixButton.setText(Messages.format(ActionMessages.ConfigureDeprecationFixDialog_InliningRadioButtonLabel, memberName));
			data= new GridData();
			data.horizontalIndent= convertVerticalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING * 2);
			fInlineFixButton.setLayoutData(data);

			fReplaceInvocationButton= new Button(composite, SWT.RADIO);
			fReplaceInvocationButton.setText(Messages.format(ActionMessages.ConfigureDeprecationFixDialog_ReplaceReferencesRadioButtonLabel, memberName));
			data= new GridData();
			data.horizontalIndent= convertVerticalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING * 2);
			fReplaceInvocationButton.setLayoutData(data);

			fBodyEditorControl= createBody(composite);
			data= new GridData(GridData.FILL_BOTH);
			data.widthHint= pixelConverter.convertWidthInCharsToPixels(60);
			data.minimumHeight= pixelConverter.convertHeightInCharsToPixels(5);
			data.horizontalIndent= convertVerticalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING * 4);
			fBodyEditorControl.setLayoutData(data);


			fNoFixButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					if (fNoFixButton.getSelection()) {
						if (fInlineFixButton.getSelection()) {
							fFutureFix= INLINE_FIX;
							fBodyEditorControl.setEnabled(false);
						} else {
							fFutureFix= REPLACE_FIX;
							fBodyEditorControl.setEnabled(true);
						}
						fInlineFixButton.setEnabled(true);
						fReplaceInvocationButton.setEnabled(true);
					} else {
						fFutureFix= NO_FIX;
						fInlineFixButton.setEnabled(false);
						fReplaceInvocationButton.setEnabled(false);
						fBodyEditorControl.setEnabled(false);
					}
					updateStatus();
				}
			});

			fReplaceInvocationButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					fFutureFix= REPLACE_FIX;
					fBodyEditorControl.setEnabled(true);
					updateStatus();
				}
			});

			fInlineFixButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(final SelectionEvent event) {
					fFutureFix= INLINE_FIX;
					fBodyEditorControl.setEnabled(false);
					updateStatus();
				}
			});

			initializeFix();
			initializeControls();
		} else {
			fReplaceInvocationButton= new Button(composite, SWT.CHECK);
			fReplaceInvocationButton.setText(Messages.format(ActionMessages.ConfigureDeprecationFixDialog_ReplaceReferencesCheckBoxLabel, memberName));
			data= new GridData();
			data.verticalIndent= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING * 2);
			fReplaceInvocationButton.setLayoutData(data);

			fBodyEditorControl= createBody(composite);
			data= new GridData(GridData.FILL_BOTH);
			data.widthHint= pixelConverter.convertWidthInCharsToPixels(50);
			data.minimumHeight= pixelConverter.convertHeightInCharsToPixels(5);
			data.horizontalIndent= convertVerticalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING * 2);
			fBodyEditorControl.setLayoutData(data);

			fReplaceInvocationButton.addSelectionListener(new SelectionAdapter() {

				public void widgetSelected(SelectionEvent e) {
					if (fReplaceInvocationButton.getSelection()) {
						fFutureFix= REPLACE_FIX;
						fBodyEditorControl.setEnabled(true);
					} else {
						fFutureFix= NO_FIX;
						fBodyEditorControl.setEnabled(false);
					}
					updateStatus();
				}
			});

			initializeFix();
			switch (fCurrentFix) {
				case NO_FIX:
					fReplaceInvocationButton.setSelection(false);
					fBodyEditorControl.setEnabled(false);
					break;
				case REPLACE_FIX:
					fReplaceInvocationButton.setSelection(true);
					fBodyEditorControl.setEnabled(true);
					break;
			}
		}

		applyDialogFont(composite);

		return composite;
	}

	private Control createBody(Composite parent) {
		IPreferenceStore store= JavaPlugin.getDefault().getCombinedPreferenceStore();
		JavaSourceViewer bodyEditor= new JavaSourceViewer(parent, null, null, false, SWT.V_SCROLL | SWT.WRAP | SWT.BORDER, store);
		bodyEditor.configure(new JavaSourceViewerConfiguration(JavaPlugin.getDefault().getJavaTextTools().getColorManager(), store, null, null));
		bodyEditor.getTextWidget().setFont(JFaceResources.getFont(PreferenceConstants.EDITOR_TEXT_FONT));
		Document bodyDocument= new Document(""); //$NON-NLS-1$
		bodyEditor.setDocument(bodyDocument);
		bodyEditor.setEditable(true);

		return bodyEditor.getControl();
	}

	private void updateStatus() {
		StatusInfo status= new StatusInfo();
		if (fFutureFix == REPLACE_FIX) {
			status.setInfo(ActionMessages.ConfigureDeprecationFixDialog_ThisFeatureIsNotSupportedInfo);
			updateStatus(status);
			status.setError(""); //$NON-NLS-1$
			updateButtonsEnableState(status);
		} else {
			updateStatus(status);			
		}
	}

	private boolean canInlineMember() {
		int modifiers= fBinding.getModifiers();
		if (Modifier.isAbstract(modifiers))
			return false;

		if (fBinding instanceof IVariableBinding) {
			IVariableBinding varBining= (IVariableBinding)fBinding;
			if (varBining.isField()) {
				if (!Modifier.isFinal(modifiers))
					return false;
			} else if (varBining.isEnumConstant()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Initializes the controls of this dialog.
	 */
	private void initializeControls() {
		switch (fCurrentFix) {
			case NO_FIX:
				fNoFixButton.setSelection(false);
				fInlineFixButton.setSelection(true);
				fReplaceInvocationButton.setSelection(false);
	
				fInlineFixButton.setEnabled(false);
				fReplaceInvocationButton.setEnabled(false);
				fBodyEditorControl.setEnabled(false);
				break;
			case INLINE_FIX:
				fNoFixButton.setSelection(true);
				fInlineFixButton.setSelection(true);
				fReplaceInvocationButton.setSelection(false);
	
				fInlineFixButton.setEnabled(true);
				fReplaceInvocationButton.setEnabled(true);
				fBodyEditorControl.setEnabled(false);
				break;
			case REPLACE_FIX:
				fNoFixButton.setSelection(true);
				fInlineFixButton.setSelection(false);
				fReplaceInvocationButton.setSelection(true);
	
				fInlineFixButton.setEnabled(true);
				fReplaceInvocationButton.setEnabled(true);
				fBodyEditorControl.setEnabled(true);
				break;
			}
	}

	/**
	 * Initializes the current fix.
	 */
	private void initializeFix() {
		final String name= DeprecationRefactorings.getRefactoringScriptName(fBinding);
		if (name != null) {
			final IFile file= DeprecationRefactorings.getRefactoringScriptFile(fProject, name);
			if (file != null && file.exists()) {
				final RefactoringHistory history= DeprecationRefactorings.getRefactoringHistory(file);
				if (history != null) {
					final RefactoringDescriptorProxy[] proxies= history.getDescriptors();
					if (proxies.length == 1) {
						final RefactoringDescriptor descriptor= proxies[0].requestDescriptor(null);
						if (descriptor != null && descriptor.getID().equals(InlineMethodRefactoring.ID_INLINE_METHOD))
							fCurrentFix= INLINE_FIX;
					}
				}
			}
		}
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