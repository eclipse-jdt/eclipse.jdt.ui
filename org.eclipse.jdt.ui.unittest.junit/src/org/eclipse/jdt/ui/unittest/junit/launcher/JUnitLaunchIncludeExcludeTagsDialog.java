/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
 */
package org.eclipse.jdt.ui.unittest.junit.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.StatusDialog;

import org.eclipse.debug.core.ILaunchConfiguration;

import org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TextBoxDialogField;

public class JUnitLaunchIncludeExcludeTagsDialog extends StatusDialog {
	private SelectionButtonDialogField fHasIncludeTags;

	private SelectionButtonDialogField fHasExcludeTags;

	private TextBoxDialogField fIncludeTags;

	private TextBoxDialogField fExcludeTags;

	private ILaunchConfiguration fLaunchConfiguration;

	private static final String EMPTY_STRING = ""; //$NON-NLS-1$

	IDialogFieldListener fListener = this::doDialogFieldChanged;

	public JUnitLaunchIncludeExcludeTagsDialog(Shell parent, ILaunchConfiguration config) {
		super(parent);
		fLaunchConfiguration = config;
		setTitle(Messages.JUnitLaunchConfigurationTab_addincludeexcludetagdialog_title);
		createIncludeTagGroup();
		createExcludeTagGroup();
		setHelpAvailable(false);
		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);

		Composite inner = new Composite(composite, SWT.NONE);
		inner.setFont(composite.getFont());

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 1;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));

		fHasIncludeTags.doFillIntoGrid(inner, 1);
		fIncludeTags.doFillIntoGrid(inner, 2);
		LayoutUtil.setHorizontalIndent(fIncludeTags.getLabelControl(null));
		Text includeTagControl = fIncludeTags.getTextControl(null);
		LayoutUtil.setHorizontalIndent(includeTagControl);
		LayoutUtil.setHorizontalGrabbing(includeTagControl);
		LayoutUtil.setWidthHint(fIncludeTags.getLabelControl(null), convertWidthInCharsToPixels(50));
		LayoutUtil.setVerticalGrabbing(includeTagControl);
		LayoutUtil.setHeightHint(includeTagControl, convertHeightInCharsToPixels(3));

		fHasExcludeTags.doFillIntoGrid(inner, 1);
		fExcludeTags.doFillIntoGrid(inner, 2);
		LayoutUtil.setHorizontalIndent(fExcludeTags.getLabelControl(null));
		Text excludeTagsControl = fExcludeTags.getTextControl(null);
		LayoutUtil.setHorizontalIndent(excludeTagsControl);
		LayoutUtil.setHorizontalGrabbing(excludeTagsControl);
		LayoutUtil.setWidthHint(fExcludeTags.getLabelControl(null), convertWidthInCharsToPixels(50));
		LayoutUtil.setVerticalGrabbing(excludeTagsControl);
		LayoutUtil.setHeightHint(excludeTagsControl, convertHeightInCharsToPixels(3));

		applyDialogFont(composite);
		return composite;
	}

	private String getCommaSeperatedText(String input) {
		if (input.isEmpty())
			return EMPTY_STRING;
		StringBuilder buf = new StringBuilder();
		String[] strings = input.split(System.lineSeparator());
		for (int i = 0; i < strings.length; i++) {
			if (i > 0)
				buf.append(',');
			buf.append(strings[i]);
		}
		return buf.toString();
	}

	private String getLineSeperatedText(String input) {
		if (input.isEmpty())
			return EMPTY_STRING;
		StringBuilder buf = new StringBuilder();
		String[] strings = input.split(","); //$NON-NLS-1$
		for (int i = 0; i < strings.length; i++) {
			if (i > 0)
				buf.append(System.lineSeparator());
			buf.append(strings[i]);
		}
		return buf.toString();
	}

	public String getIncludeTags() {
		return getCommaSeperatedText(fIncludeTags.getText());
	}

	public String getExcludeTags() {
		return getCommaSeperatedText(fExcludeTags.getText());
	}

	public boolean hasIncludeTags() {
		return fHasIncludeTags.isSelected();
	}

	public boolean hasExcludeTags() {
		return fHasExcludeTags.isSelected();
	}

	private void createIncludeTagGroup() {
		fHasIncludeTags = new SelectionButtonDialogField(SWT.CHECK);
		fHasIncludeTags.setDialogFieldListener(fListener);
		fHasIncludeTags.setLabelText(Messages.JUnitLaunchConfigurationTab_includetag_checkbox_label);

		fIncludeTags = new TextBoxDialogField();
		fIncludeTags.setDialogFieldListener(fListener);
		fIncludeTags.setLabelText(Messages.JUnitLaunchConfigurationTab_includetags_description);

		try {
			fHasIncludeTags.setSelection(fLaunchConfiguration
					.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_INCLUDE_TAGS, false));
		} catch (CoreException e) {
			// ignore
		}
		fIncludeTags.setEnabled(fHasIncludeTags.isSelected());
		try {
			fIncludeTags.setText(getLineSeperatedText(fLaunchConfiguration
					.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS, EMPTY_STRING)));
		} catch (CoreException e) {
			// ignore
		}
	}

	private void createExcludeTagGroup() {
		fHasExcludeTags = new SelectionButtonDialogField(SWT.CHECK);
		fHasExcludeTags.setDialogFieldListener(fListener);
		fHasExcludeTags.setLabelText(Messages.JUnitLaunchConfigurationTab_excludetag_checkbox_label);

		fExcludeTags = new TextBoxDialogField();
		fExcludeTags.setDialogFieldListener(fListener);
		fExcludeTags.setLabelText(Messages.JUnitLaunchConfigurationTab_excludetags_description);

		try {
			fHasExcludeTags.setSelection(fLaunchConfiguration
					.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_HAS_EXCLUDE_TAGS, false));
		} catch (CoreException e) {
			// ignore
		}
		fExcludeTags.setEnabled(fHasExcludeTags.isSelected());
		try {
			fExcludeTags.setText(getLineSeperatedText(fLaunchConfiguration
					.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_EXCLUDE_TAGS, EMPTY_STRING)));
		} catch (CoreException e) {
			// ignore
		}
	}

	private IStatus getValidationStatus() {
		if (fHasIncludeTags != null && fHasIncludeTags.isSelected()
				&& fIncludeTags.getText().trim().equals(EMPTY_STRING)) {
			return new StatusInfo(IStatus.ERROR, Messages.JUnitLaunchConfigurationTab_includetag_empty_error);
		}

		if (fHasExcludeTags != null && fHasExcludeTags.isSelected()
				&& fExcludeTags.getText().trim().equals(EMPTY_STRING))
			return new StatusInfo(IStatus.ERROR, Messages.JUnitLaunchConfigurationTab_excludetag_empty_error);

		return new StatusInfo();
	}

	private void doDialogFieldChanged(DialogField field) {
		if (field == fHasIncludeTags) {
			fIncludeTags.setEnabled(fHasIncludeTags.isSelected());
		}
		if (field == fHasExcludeTags) {
			fExcludeTags.setEnabled(fHasExcludeTags.isSelected());
		}
		updateStatus(getValidationStatus());
	}

}
