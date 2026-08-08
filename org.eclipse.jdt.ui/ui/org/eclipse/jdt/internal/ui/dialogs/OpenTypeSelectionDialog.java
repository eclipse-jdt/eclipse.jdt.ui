/*******************************************************************************
 * Copyright (c) 2000, 2026 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.dialogs;


import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ui.ActiveShellExpression;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.corext.util.TypeInfoFilter;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.dialogs.TypeSelectionExtension;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;

/**
 * A type selection dialog used for opening types.
 */
public class OpenTypeSelectionDialog extends FilteredTypesSelectionDialog {

	private static final String DIALOG_SETTINGS= "org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog2"; //$NON-NLS-1$

	private SelectionButtonDialogField fOpenTypesInferWildcards;

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds) {
		this(parent, multi, context, scope, elementKinds, null);
	}

	public OpenTypeSelectionDialog(Shell parent, boolean multi, IRunnableContext context, IJavaSearchScope scope, int elementKinds, TypeSelectionExtension extension) {
		super(parent, multi, context, scope, elementKinds, extension);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell, IJavaHelpContextIds.OPEN_TYPE_DIALOG);
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);

		if (settings == null) {
			settings= JavaPlugin.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		}

		return settings;
	}

	private void doDialogFieldChanged(DialogField field) {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();

		if (field == fOpenTypesInferWildcards)
			preferenceStore.setValue(PreferenceConstants.OPEN_TYPES_INFER_WILDCARDS, fOpenTypesInferWildcards.isSelected());
		applyFilter();
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		IPreferenceStore preferenceStore= JavaPlugin.getDefault().getPreferenceStore();
		IDialogFieldListener listener= this::doDialogFieldChanged;

		Composite newComposite= new Composite(parent, NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 1;
		layout.marginLeft= 5;
		newComposite.setLayout(layout);
		GridData gd= new GridData();
		gd.horizontalIndent= 5;
		newComposite.setLayoutData(gd);
		fOpenTypesInferWildcards= new SelectionButtonDialogField(SWT.CHECK);
		fOpenTypesInferWildcards.setDialogFieldListener(listener);
		fOpenTypesInferWildcards.setLabelText(PreferencesMessages.OpenTypesDialog_default_wildcard_between_camel_case_parts_label);
		fOpenTypesInferWildcards.doFillIntoGrid(newComposite, 1);
		fOpenTypesInferWildcards.setSelection(preferenceStore.getBoolean(PreferenceConstants.OPEN_TYPES_INFER_WILDCARDS));

		return super.createButtonBar(parent);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (getClass() == OpenTypeSelectionDialog.class) {
			addCommand(IWorkbenchCommandConstants.EDIT_PASTE, getPatternControl(), () -> {
				Text text= (Text) getPatternControl();
				Clipboard clipboard= new Clipboard(text.getDisplay());
				try {
					String clipText= (String) clipboard.getContents(TextTransfer.getInstance());
					try {
						String toInsert= TypeInfoFilter.simplifySearchText(clipText);
						clipboard.setContents(new Object[] { toInsert },
								new Transfer[] { TextTransfer.getInstance() });
						text.paste();
					} finally {
						clipboard.setContents(new Object[] { clipText },
								new Transfer[] { TextTransfer.getInstance() });
					}
				} finally {
					clipboard.dispose();
				}
			});
			createButton(parent, IDialogConstants.OK_ID, JavaUIMessages.OpenTypeSelectionDialog_open, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		} else
			super.createButtonsForButtonBar(parent);
	}

	private static void addCommand(String commandId, Control control, Runnable runnable) {
		IHandlerService hs= PlatformUI.getWorkbench().getService(IHandlerService.class);
		if (hs == null) {
			return;
		}
		IAction action= new Action("any_" + commandId) { //$NON-NLS-1$
			@Override
			public void run() {
				if (control.isDisposed()) {
					return;
				}
				runnable.run();
			}
		};
		IHandlerActivation handlerActivation= hs.activateHandler(commandId, new ActionHandler(action),
				new ActiveShellExpression(control.getShell()));
		control.addDisposeListener(e -> hs.deactivateHandler(handlerActivation));
	}

}
