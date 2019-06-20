/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jdt.internal.ui.util.SWTUtil;

/**
 * Dialog Field containing a single button such as a radio or checkbox button.
 */
public class SelectionButtonDialogField extends DialogField {

	private Button fButton;
	private boolean fIsSelected;
	private DialogField[] fAttachedDialogFields;
	private int fButtonStyle;

	/**
	 * Creates a selection button.
	 * Allowed button styles: SWT.RADIO, SWT.CHECK, SWT.TOGGLE, SWT.PUSH
	 */
	public SelectionButtonDialogField(int buttonStyle) {
		super();
		fIsSelected= false;
		fAttachedDialogFields= null;
		fButtonStyle= buttonStyle;
	}

	/**
	 * Attaches a field to the selection state of the selection button.
	 * The attached field will be disabled if the selection button is not selected.
	 */
	public void attachDialogField(DialogField dialogField) {
		attachDialogFields(new DialogField[] { dialogField });
	}

	/**
	 * Attaches fields to the selection state of the selection button.
	 * The attached fields will be disabled if the selection button is not selected.
	 */
	public void attachDialogFields(DialogField[] dialogFields) {
		fAttachedDialogFields= dialogFields;
		for (DialogField dialogField : dialogFields) {
			dialogField.setEnabled(fIsSelected);
		}
	}

	/**
	 * Returns <code>true</code> is  teh gived field is attached to the selection button.
	 */
	public boolean isAttached(DialogField editor) {
		if (fAttachedDialogFields != null) {
			for (DialogField dialogField : fAttachedDialogFields) {
				if (dialogField == editor) {
					return true;
				}
			}
		}
		return false;
	}

	// ------- layout helpers

	/*
	 * @see DialogField#doFillIntoGrid
	 */
	@Override
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		assertEnoughColumns(nColumns);

		Button button= getSelectionButton(parent);
		GridData gd= new GridData();
		gd.horizontalSpan= nColumns;
		gd.horizontalAlignment= GridData.FILL;
		if (fButtonStyle == SWT.PUSH) {
			gd.widthHint = SWTUtil.getButtonWidthHint(button);
		}

		button.setLayoutData(gd);

		return new Control[] { button };
	}


	public Control[] doFillIntoGridWithoutMargin(Composite parent, int nColumns, boolean indent) {
		assertEnoughColumns(nColumns);
		GridData gd= new GridData(GridData.FILL, GridData.FILL, true, false);
		gd.horizontalSpan= 3;
		gd.horizontalIndent= 0;
		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.horizontalSpacing= 0;
		layout.numColumns= nColumns;
		parent.setLayout(layout);
		parent.setLayoutData(gd);

		GridData gridData= new GridData(GridData.FILL, GridData.CENTER, false, false);
		if(indent)
			gridData.horizontalIndent= 20;
		Button button= getSelectionButton(parent);
		button.setLayoutData(gridData);
		return new Control[] { button };
	}

	/*
	 * @see DialogField#getNumberOfControls
	 */
	@Override
	public int getNumberOfControls() {
		return 1;
	}

	// ------- ui creation

	/**
	 * Returns the selection button widget. When called the first time, the widget will be created.
	 * @param group The parent composite when called the first time, or <code>null</code>
	 * after.
	 */
	public Button getSelectionButton(Composite group) {
		if (fButton == null) {
			assertCompositeNotNull(group);

			fButton= new Button(group, fButtonStyle);
			fButton.setFont(group.getFont());
			fButton.setText(fLabelText);
			fButton.setEnabled(isEnabled());
			fButton.setSelection(fIsSelected);
			fButton.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					doWidgetSelected(e);
				}
				@Override
				public void widgetSelected(SelectionEvent e) {
					doWidgetSelected(e);
				}
			});
		}
		return fButton;
	}

	private void doWidgetSelected(SelectionEvent e) {
		if (isOkToUse(fButton)) {
			changeValue(fButton.getSelection());
		}
	}

	private void changeValue(boolean newState) {
		if (fIsSelected != newState) {
			fIsSelected= newState;
			if (fAttachedDialogFields != null) {
				boolean focusSet= false;
				for (DialogField dialogField : fAttachedDialogFields) {
					dialogField.setEnabled(fIsSelected);
					if (fIsSelected && !focusSet) {
						focusSet= dialogField.setFocus();
					}
				}
			}
			dialogFieldChanged();
		} else if (fButtonStyle == SWT.PUSH) {
			dialogFieldChanged();
		}
	}

	@Override
	public void setLabelText(String labeltext) {
		fLabelText= labeltext;
		if (isOkToUse(fButton)) {
			fButton.setText(labeltext);
		}
	}


	// ------ model access

	/**
	 * Returns the selection state of the button.
	 */
	public boolean isSelected() {
		return fIsSelected;
	}

	/**
	 * Sets the selection state of the button.
	 */
	public void setSelection(boolean selected) {
		changeValue(selected);
		if (isOkToUse(fButton)) {
			fButton.setSelection(selected);
		}
	}

	// ------ enable / disable management

	/*
	 * @see DialogField#updateEnableState
	 */
	@Override
	protected void updateEnableState() {
		super.updateEnableState();
		if (isOkToUse(fButton)) {
			fButton.setEnabled(isEnabled());
		}
	}

	@Override
	public void refresh() {
		super.refresh();
		if (isOkToUse(fButton)) {
			fButton.setSelection(fIsSelected);
		}
	}


}
