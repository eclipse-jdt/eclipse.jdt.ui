/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package org.eclipse.jdt.internal.junit.launcher;

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

import org.eclipse.jdt.internal.junit.ui.JUnitMessages;
import org.eclipse.jdt.internal.junit.util.LayoutUtil;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TextBoxDialogField;

public class JUnitLaunchIncludeExcludeTagsDialog extends StatusDialog {
	private SelectionButtonDialogField fIsIncludeTags;

	private SelectionButtonDialogField fIsExcludeTags;

	private TextBoxDialogField fIncludeTags;

	private TextBoxDialogField fExcludeTags;

	private ILaunchConfiguration fLaunchConfiguration;

	private static final String EMPTY_STRING= ""; //$NON-NLS-1$

	IDialogFieldListener fListener= new IDialogFieldListener() {
		@Override
		public void dialogFieldChanged(DialogField field) {
			doDialogFieldChanged(field);
		}
	};

	public JUnitLaunchIncludeExcludeTagsDialog(Shell parent, ILaunchConfiguration config) {
		super(parent);
		fLaunchConfiguration= config;
		setTitle(JUnitMessages.JUnitLaunchConfigurationTab_addincludeexcludetagdialog_title);
		createIncludeTagGroup();
		createExcludeTagGroup();
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite) super.createDialogArea(parent);

		Composite inner= new Composite(composite, SWT.NONE);
		inner.setFont(composite.getFont());

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.numColumns= 1;
		inner.setLayout(layout);
		inner.setLayoutData(new GridData(GridData.FILL_BOTH));

		fIsIncludeTags.doFillIntoGrid(inner, 1);
		fIncludeTags.doFillIntoGrid(inner, 2);
		LayoutUtil.setHorizontalIndent(fIncludeTags.getLabelControl(null));
		Text includeTagControl= fIncludeTags.getTextControl(null);
		LayoutUtil.setHorizontalIndent(includeTagControl);
		LayoutUtil.setHorizontalGrabbing(includeTagControl);
		LayoutUtil.setWidthHint(fIncludeTags.getLabelControl(null), convertWidthInCharsToPixels(50));
		LayoutUtil.setVerticalGrabbing(includeTagControl);
		LayoutUtil.setHeightHint(includeTagControl, convertHeightInCharsToPixels(3));

		fIsExcludeTags.doFillIntoGrid(inner, 1);
		fExcludeTags.doFillIntoGrid(inner, 2);
		LayoutUtil.setHorizontalIndent(fExcludeTags.getLabelControl(null));
		Text excludeTagsControl= fExcludeTags.getTextControl(null);
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
		StringBuilder buf= new StringBuilder();
		String[] strings= input.split(System.lineSeparator());
		for (int i= 0; i < strings.length; i++) {
			if (i > 0)
				buf.append(',');
			buf.append(strings[i]);
		}
		return buf.toString();
	}

	private String getLineSeperatedText(String input) {
		if (input.isEmpty())
			return EMPTY_STRING;
		StringBuilder buf= new StringBuilder();
		String[] strings= input.split(","); //$NON-NLS-1$
		for (int i= 0; i < strings.length; i++) {
			if (i > 0)
				buf.append(System.lineSeparator());
			buf.append(strings[i]);
		}
		return buf.toString();
	}

	/*
	 * Gets the Included tags
	 */
	public String getIncludeTags() {
		return getCommaSeperatedText(fIncludeTags.getText());
	}

	/*
	 * Gets the Excluded tags
	 */
	public String getExcludeTags() {
		return getCommaSeperatedText(fExcludeTags.getText());
	}

	/*
	 * If Included tags are present
	 */
	public boolean isIncludeTags() {
		return fIsIncludeTags.isSelected();
	}

	/*
	 * If Excluded tags are present
	 */
	public boolean isExcludeTags() {
		return fIsExcludeTags.isSelected();
	}


	private void createIncludeTagGroup() {

		fIsIncludeTags= new SelectionButtonDialogField(SWT.CHECK);
		fIsIncludeTags.setDialogFieldListener(fListener);
		fIsIncludeTags.setLabelText(JUnitMessages.JUnitLaunchConfigurationTab_includetag_checkbox_label);

		fIncludeTags= new TextBoxDialogField();
		fIncludeTags.setDialogFieldListener(fListener);
		fIncludeTags.setLabelText(JUnitMessages.JUnitLaunchConfigurationTab_includetags_description);

		try {
			fIsIncludeTags.setSelection(fLaunchConfiguration.getAttribute(org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.ATTR_TEST_IS_INCLUDE_TAG, false));
		} catch (CoreException e) {
		}
		fIncludeTags.setEnabled(fIsIncludeTags.isSelected());
		try {
			fIncludeTags
					.setText(getLineSeperatedText(fLaunchConfiguration.getAttribute(org.eclipse.jdt.internal.junit.launcher.JUnitLaunchConfigurationConstants.ATTR_TEST_INCLUDE_TAGS, EMPTY_STRING)));
		} catch (CoreException e) {
		}


	}

	private void createExcludeTagGroup() {
		fIsExcludeTags= new SelectionButtonDialogField(SWT.CHECK);
		fIsExcludeTags.setDialogFieldListener(fListener);
		fIsExcludeTags.setLabelText(JUnitMessages.JUnitLaunchConfigurationTab_excludetag_checkbox_label);

		fExcludeTags= new TextBoxDialogField();
		fExcludeTags.setDialogFieldListener(fListener);
		fExcludeTags.setLabelText(JUnitMessages.JUnitLaunchConfigurationTab_excludetags_description);

		try {
			fIsExcludeTags.setSelection(fLaunchConfiguration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_IS_EXCLUDE_TAG, false));
		} catch (CoreException e) {
		}
		fExcludeTags.setEnabled(fIsExcludeTags.isSelected());
		try {
			fExcludeTags
					.setText(getLineSeperatedText(fLaunchConfiguration.getAttribute(JUnitLaunchConfigurationConstants.ATTR_TEST_EXCLUDE_TAGS, EMPTY_STRING)));
		} catch (CoreException e) {
		}
	}


	private IStatus getValidationStatus() {
		if (fIsIncludeTags != null && fIsIncludeTags.isSelected()
				&& fIncludeTags.getText().equals("")) { //$NON-NLS-1$
			return new StatusInfo(IStatus.ERROR, JUnitMessages.JUnitLaunchConfigurationTab_includetag_empty_error);
		}

		if (fIsExcludeTags != null && fIsExcludeTags.isSelected() && fExcludeTags.getText().equals(EMPTY_STRING))
			return new StatusInfo(IStatus.ERROR, JUnitMessages.JUnitLaunchConfigurationTab_excludetag_empty_error);

		return new StatusInfo();
	}

	private void doDialogFieldChanged(DialogField field) {
		if (field == fIsIncludeTags) {
			fIncludeTags.setEnabled(fIsIncludeTags.isSelected());
		}
		if (field == fIsExcludeTags) {
			fExcludeTags.setEnabled(fIsExcludeTags.isSelected());
		}
		updateStatus(getValidationStatus());
	}

}
