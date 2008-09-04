/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.junit.util.LayoutUtil;

/**
 * A group of controls used in the JUnit TestCase and TestSuite wizards
 * for selecting method stubs to create.
 */
public class MethodStubsSelectionButtonGroup {

	private Label fLabel;
	protected String fLabelText;

	private SelectionButtonGroupListener fGroupListener;

	private boolean fEnabled;

	private Composite fButtonComposite;

	private Button[] fButtons;
	private String[] fButtonNames;
	private boolean[] fButtonsSelected;
	private boolean[] fButtonsEnabled;

	private int fGroupBorderStyle;
	private int fGroupNumberOfColumns;
	private int fButtonsStyle;


	public interface SelectionButtonGroupListener {
		/**
		 * The dialog field has changed.
		 * @param field The changed dialog field
		 */
		void groupChanged(MethodStubsSelectionButtonGroup field);
	}

	/**
	 * Creates a group without border.
	 * @param buttonsStyle one of {@link SWT#RADIO}, {@link SWT#CHECK}, or {@link SWT#TOGGLE}
	 * @param buttonNames names of the buttons
	 * @param nColumns column count
	 */
	public MethodStubsSelectionButtonGroup(int buttonsStyle, String[] buttonNames, int nColumns) {
		fEnabled= true;
		fLabel= null;
		fLabelText= ""; //$NON-NLS-1$

		Assert.isTrue(buttonsStyle == SWT.RADIO || buttonsStyle == SWT.CHECK || buttonsStyle == SWT.TOGGLE);
		fButtonNames= buttonNames;

		int nButtons= buttonNames.length;
		fButtonsSelected= new boolean[nButtons];
		fButtonsEnabled= new boolean[nButtons];
		for (int i= 0; i < nButtons; i++) {
			fButtonsSelected[i]= false;
			fButtonsEnabled[i]= true;
		}

		if (buttonsStyle == SWT.RADIO) {
			fButtonsSelected[0]= true;
		}

		fGroupBorderStyle= SWT.NONE;
		fGroupNumberOfColumns= (nColumns <= 0) ? nButtons : nColumns;

		fButtonsStyle= buttonsStyle;
	}

	/*
	 * @see DialogField#doFillIntoGrid
	 */
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		assertEnoughColumns(nColumns);

		if (fGroupBorderStyle == SWT.NONE) {
			Label label= getLabelControl(parent);
			label.setLayoutData(gridDataForLabel(1));

			Composite buttonsgroup= getSelectionButtonsGroup(parent);
			GridData gd= new GridData();
			gd.horizontalSpan= nColumns - 1;
			buttonsgroup.setLayoutData(gd);
			return new Control[] { label, buttonsgroup };
		} else {
			Composite buttonsgroup= getSelectionButtonsGroup(parent);
			GridData gd= new GridData();
			gd.horizontalSpan= nColumns;
			buttonsgroup.setLayoutData(gd);
			return new Control[] { buttonsgroup };
		}
	}

	/*
	 * @see DialogField#doFillIntoGrid
	 */
	public int getNumberOfControls() {
		return (fGroupBorderStyle == SWT.NONE) ? 2 : 1;
	}

	private Button createSelectionButton(int index, Composite group, SelectionListener listener) {
		Button button= new Button(group, fButtonsStyle | SWT.LEFT);
		button.setFont(group.getFont());
		button.setText(fButtonNames[index]);
		button.setEnabled(isEnabled() && isEnabled(index));
		button.setSelection(isSelected(index));
		button.addSelectionListener(listener);
		return button;
	}


	/**
	 * Returns the group widget. When called the first time, the widget will be created.
	 * @param parent composite when called the first time, or <code>null</code>
	 * after.
	 * @return the created composite
	 */
	public Composite getSelectionButtonsGroup(Composite parent) {
		if (fButtonComposite == null) {
			assertCompositeNotNull(parent);

			GridLayout layout= new GridLayout();
			//layout.makeColumnsEqualWidth= true;
			layout.numColumns= fGroupNumberOfColumns;

			if (fGroupBorderStyle != SWT.NONE) {
				Group group= new Group(parent, fGroupBorderStyle);
				if (fLabelText != null && fLabelText.length() > 0) {
					group.setText(fLabelText);
				}
				fButtonComposite= group;
			} else {
				fButtonComposite= new Composite(parent, SWT.NULL);
				layout.marginHeight= 0;
				layout.marginWidth= 0;
			}
			fButtonComposite.setLayout(layout);

			SelectionListener listener= new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					doWidgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					doWidgetSelected(e);
				}
			};
			int nButtons= fButtonNames.length;
			fButtons= new Button[nButtons];

			for (int i= 0; i < nButtons; i++) {
				fButtons[i]= createSelectionButton(i, fButtonComposite, listener);
			}
			int nRows= nButtons / fGroupNumberOfColumns;
			int nFillElements= nRows * fGroupNumberOfColumns - nButtons;
			for (int i= 0; i < nFillElements; i++) {
				createEmptySpace(fButtonComposite);
			}
			setSelectionGroupListener(new SelectionButtonGroupListener() {
				public void groupChanged(MethodStubsSelectionButtonGroup field) {
					field.setEnabled(1, isEnabled() && field.isSelected(0));
				}
			});
		}
		return fButtonComposite;
	}

	/**
	 * Returns a button from the group or <code>null</code> if not yet created.
	 * @param index the button index
	 * @return the button
	 */
	public Button getSelectionButton(int index) {
		if (index >= 0 && index < fButtons.length) {
			return fButtons[index];
		}
		return null;
	}

	private void doWidgetSelected(SelectionEvent e) {
		Button button= (Button)e.widget;
		for (int i= 0; i < fButtons.length; i++) {
			if (fButtons[i] == button) {
				fButtonsSelected[i]= button.getSelection();
				dialogFieldChanged();
				return;
			}
		}
	}

	/**
	 * Returns the selection state of a button contained in the group.
	 * @param index of the button
	 * @return returns the selection state of a button contained in the group.
	 */
	public boolean isSelected(int index) {
		if (index >= 0 && index < fButtonsSelected.length) {
			return fButtonsSelected[index] && fButtonsEnabled[index];
		}
		return false;
	}

	/**
	 * Sets the selection state of a button contained in the group.
	 * @param index of the button
	 * @param selected the new selection state of a button
	 */
	public void setSelection(int index, boolean selected) {
		if (index >= 0 && index < fButtonsSelected.length) {
			if (fButtonsSelected[index] != selected) {
				fButtonsSelected[index]= selected;
				if (fButtons != null) {
					Button button= fButtons[index];
					if (isOkToUse(button) && button.isEnabled()) {
						button.setSelection(selected);
					}
				}
			}
		}
	}

	/**
	 * Returns the enabled state of a button contained in the group.
	 * @param index of the button
	 * @return returns the enabled state of a button contained in the group
	 */
	public boolean isEnabled(int index) {
		if (index >= 0 && index < fButtonsEnabled.length) {
			return fButtonsEnabled[index];
		}
		return false;
	}

	/**
	 * Sets the enabled state of a button contained in the group.
	 * @param index of the button
	 * @param enabled  the new enabled state of a button
	 */
	public void setEnabled(int index, boolean enabled) {
		if (index >= 0 && index < fButtonsEnabled.length) {
			if (fButtonsEnabled[index] != enabled) {
				fButtonsEnabled[index]= enabled;
				if (fButtons != null) {
					Button button= fButtons[index];
					if (isOkToUse(button)) {
						button.setEnabled(enabled);
						if (!enabled) {
							button.setSelection(false);
						} else {
							button.setSelection(fButtonsSelected[index]);
						}
					}
				}
			}
		}
	}

	protected void updateEnableState() {
		if (fLabel != null) {
			fLabel.setEnabled(fEnabled);
		}
		if (fButtons != null) {
			boolean enabled= isEnabled();
			for (int i= 0; i < fButtons.length; i++) {
				Button button= fButtons[i];
				if (isOkToUse(button)) {
					button.setEnabled(enabled && fButtonsEnabled[i]);
				}
			}
		}
	}

	/**
	 * Sets the label of the dialog field.
	 * @param labeltext the text
	 */
	public void setLabelText(String labeltext) {
		fLabelText= labeltext;
	}

	/**
	 * Defines the listener for this dialog field.
	 * @param listener the listener
	 */
	public final void setSelectionGroupListener(SelectionButtonGroupListener listener) {
		fGroupListener= listener;
	}

	/**
	 * A dialog field has changed.
	 */
	public void dialogFieldChanged() {
		if (fGroupListener != null) {
			fGroupListener.groupChanged(this);
		}
	}

	/**
	 * Tries to set the focus to the dialog field.
	 * Returns <code>true</code> if the dialog field can take focus.
	 * 	To be re-implemented by dialog field implementors.
	 * @return returns <code>true</code> if the dialog field can take focus
	 */
	public boolean setFocus() {
		return false;
	}

	/**
	 * Posts <code>setFocus</code> to the display event queue.
	 * @param display the display
	 */
	public void postSetFocusOnDialogField(Display display) {
		if (display != null) {
			display.asyncExec(
				new Runnable() {
					public void run() {
						setFocus();
					}
				}
			);
		}
	}

	protected static GridData gridDataForLabel(int span) {
		GridData gd= new GridData();
		gd.horizontalSpan= span;
		return gd;
	}

	/**
	 * Creates or returns the created label widget.
	 * @param parent The parent composite or <code>null</code> if the widget has
	 * already been created.
	 * @return the label control
	 */
	public Label getLabelControl(Composite parent) {
		if (fLabel == null) {
			assertCompositeNotNull(parent);

			fLabel= new Label(parent, SWT.LEFT | SWT.WRAP);
			fLabel.setFont(parent.getFont());
			fLabel.setEnabled(fEnabled);
			if (fLabelText != null && !"".equals(fLabelText)) { //$NON-NLS-1$
				fLabel.setText(fLabelText);
			} else {
				// XXX: to avoid a 16 pixel wide empty label - revisit
				fLabel.setText("."); //$NON-NLS-1$
				fLabel.setVisible(false);
			}
		}
		return fLabel;
	}

	/**
	 * Creates a spacer control.
	 * @param parent The parent composite
	 * @return the control
	 */
	public static Control createEmptySpace(Composite parent) {
		return createEmptySpace(parent, 1);
	}

	/**
	 * Creates a spacer control with the given span.
	 * The composite is assumed to have <code>MGridLayout</code> as
	 * layout.
	 * @param parent The parent composite
	 * @param span the span
	 * @return the control
	 */
	public static Control createEmptySpace(Composite parent, int span) {
		return LayoutUtil.createEmptySpace(parent, span);
	}

	/**
	 * Tests is the control is not <code>null</code> and not disposed.
	 * @param control the control
	 * @return the result
	*/
	protected final boolean isOkToUse(Control control) {
		return (control != null) && !(control.isDisposed());
	}

	// --------- enable / disable management

	/**
	 * Sets the enable state of the dialog field.
	 * @param enabled the enabled state
	 */
	public final void setEnabled(boolean enabled) {
		if (enabled != fEnabled) {
			fEnabled= enabled;
			updateEnableState();
		}
	}

	/**
	 * Gets the enable state of the dialog field.
	 * @return returns the enabled state
	 */
	public final boolean isEnabled() {
		return fEnabled;
	}

	protected final void assertCompositeNotNull(Composite comp) {
		Assert.isNotNull(comp, "uncreated control requested with composite null"); //$NON-NLS-1$
	}

	protected final void assertEnoughColumns(int nColumns) {
		Assert.isTrue(nColumns >= getNumberOfControls(), "given number of columns is too small"); //$NON-NLS-1$
	}
}
