/*******************************************************************************
 * Copyright (c) 2005, 2021 IBM Corporation and others.
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
 *     Pierre-Yves B. <pyvesdev@gmail.com> - Generation of equals and hashcode with java 7 Objects.equals and Objects.hashcode - https://bugs.eclipse.org/424214
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.IVariableBinding;

import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.viewsupport.BindingLabelProvider;

/**
 * Dialog for the generate hashCode() and equals() action.
 *
 * @since 3.2
 */
public class GenerateHashCodeEqualsDialog extends SourceActionDialog {

	/**
	 * Content provider for the generate hashCode() and equals() tree viewer.
	 *
	 * @since 3.2
	 *
	 */
	private static class GenerateHashCodeEqualsContentProvider implements ITreeContentProvider {

		IVariableBinding[] fBindings;

		public GenerateHashCodeEqualsContentProvider(IVariableBinding[] allFields) {
			this.fBindings= allFields;
		}

		@Override
		public void dispose() {
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return fBindings;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	/**
	 * Validator for the input of the generate hashCode() and equals() dialog.
	 *
	 * @since 3.2
	 *
	 */
	private static class GenerateHashCodeEqualsValidator implements ISelectionStatusValidator {

		private static int fNumFields;

		public GenerateHashCodeEqualsValidator(int entries) {
			fNumFields= entries;
		}

		@Override
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (Object s : selection) {
				if (s instanceof IVariableBinding) {
					count++;
				}
			}
			if (count == 0)
				return new StatusInfo(IStatus.ERROR, JavaUIMessages.GenerateHashCodeEqualsDialog_select_at_least_one_field);

			return new StatusInfo(IStatus.INFO, Messages.format(JavaUIMessages.GenerateHashCodeEqualsDialog_selectioninfo_more, new String[] { String.valueOf(count), String.valueOf(fNumFields)}));
		}
	}

	private static final String SETTINGS_INSTANCEOF= "InstanceOf"; //$NON-NLS-1$
	private static final String SETTINGS_BLOCKS= "Blocks"; //$NON-NLS-1$
	private static final String SETTINGS_J7_HASH_EQUALS= "Objects.equals & Objects.hash"; //$NON-NLS-1$

	private boolean fUseInstanceOf;
	private boolean fUseBlocks;
	private boolean fUseJ7HashEquals;
	private IJavaProject fProject;

	private boolean fNoFields;

	public GenerateHashCodeEqualsDialog(Shell shell, CompilationUnitEditor editor, IType type, IVariableBinding[] allFields, IVariableBinding[] selectedFields) throws JavaModelException {
		super(shell, new BindingLabelProvider(), new GenerateHashCodeEqualsContentProvider(allFields), editor, type, false);
		this.fProject = type.getJavaProject();
		setEmptyListMessage(JavaUIMessages.GenerateHashCodeEqualsDialog_no_entries);

		setInitialSelections((Object[]) selectedFields);

		setTitle(JavaUIMessages.GenerateHashCodeEqualsDialog_dialog_title);
		setMessage(JavaUIMessages.GenerateHashCodeEqualsDialog_select_fields_to_include);
		fNoFields= allFields.length == 0;
		setValidator(new GenerateHashCodeEqualsValidator(allFields.length));
		setSize(60, 18);
		setInput(new Object());

		fUseInstanceOf= asBoolean(getDialogSettings().get(SETTINGS_INSTANCEOF), false);
		fUseBlocks= asBoolean(getDialogSettings().get(SETTINGS_BLOCKS), false);
		fUseJ7HashEquals= asBoolean(getDialogSettings().get(SETTINGS_J7_HASH_EQUALS), JavaModelUtil.is1d7OrHigher(this.fProject));

	}

	@Override
	public boolean close() {
		getDialogSettings().put(SETTINGS_INSTANCEOF, fUseInstanceOf);
		getDialogSettings().put(SETTINGS_BLOCKS, fUseBlocks);
		if (JavaModelUtil.is1d7OrHigher(this.fProject))
			getDialogSettings().put(SETTINGS_J7_HASH_EQUALS, fUseJ7HashEquals);
		return super.close();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(shell, IJavaHelpContextIds.GENERATE_HASHCODE_EQUALS_SELECTION_DIALOG);
	}

	@Override
	protected Composite createCommentSelection(final Composite parent) {
		final Composite composite= super.createCommentSelection(parent);

		Button button= new Button(composite, SWT.CHECK);
		button.setText(JavaUIMessages.GenerateHashCodeEqualsDialog_instanceof_button);

		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				setUseInstanceOf((((Button) event.widget).getSelection()));
			}
		});
		button.setSelection(isUseInstanceOf());
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan= 2;
		button.setLayoutData(data);

		button= new Button(composite, SWT.CHECK);
		button.setText(JavaUIMessages.GenerateHashCodeEqualsDialog_blocks_button);

		button.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent event) {
				setUseBlocks((((Button) event.widget).getSelection()));
			}
		});
		button.setSelection(isUseBlocks());

		button= new Button(composite, SWT.CHECK);
		button.setText(JavaUIMessages.GenerateHashCodeEqualsDialog_j7hashequals_button);
		if (JavaModelUtil.is1d7OrHigher(this.fProject)) {
			button.addSelectionListener(new SelectionAdapter() {

				@Override
				public void widgetSelected(SelectionEvent event) {
					setUseJ7HashEquals((((Button) event.widget).getSelection()));
				}
			});
			button.setSelection(isUseJ7HashEquals());
		} else {
			button.setEnabled(false);
			button.setSelection(false);
			setUseJ7HashEquals(false);
		}
		data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan= 2;
		button.setLayoutData(data);

		return composite;
	}

	public boolean isUseInstanceOf() {
		return fUseInstanceOf;
	}

	public void setUseInstanceOf(boolean use) {
		fUseInstanceOf= use;
	}

	public boolean isUseBlocks() {
		return fUseBlocks;
	}

	public void setUseBlocks(boolean useBlocks) {
		fUseBlocks= useBlocks;
	}

	public boolean isUseJ7HashEquals() {
		return fUseJ7HashEquals;
	}

	public void setUseJ7HashEquals(boolean useJ7HashEquals) {
		fUseJ7HashEquals= useJ7HashEquals;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, JavaUIMessages.GenerateHashCodeEqualsDialog_generate, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void updateOKStatus() {
		if(fNoFields) {
			updateStatus(new Status(IStatus.OK, PlatformUI.PLUGIN_ID,
					IStatus.OK, "", //$NON-NLS-1$
					null));
			return;
		}
		super.updateOKStatus();
	}
}