/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.util.Assert;

import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;

public class SelectionButtonDialogFieldGroup extends DialogField {
	
	private Composite fButtonComposite;
	
	private Button[] fButtons;
	private String[] fButtonNames;
	private boolean[] fButtonsSelected;
	private boolean[] fButtonsEnabled;
	
	private int fGroupBorderStyle;
	private int fGroupNumberOfColumns;
	private int fButtonsStyle;	
		
	/**
	 * create without border
	 */
	public SelectionButtonDialogFieldGroup(int buttonsStyle, String[] buttonNames, int nColumns) {
		this(buttonsStyle, buttonNames, nColumns, -1);		
	}	
	
	
	/**
	 * create with border (label in border)
	 */	
	public SelectionButtonDialogFieldGroup(int buttonsStyle, String[] buttonNames, int nColumns, int borderStyle) {
		super();
		
		Assert.isTrue(buttonsStyle == SWT.RADIO || buttonsStyle == SWT.CHECK || buttonsStyle == SWT.TOGGLE);
		fButtonNames= buttonNames;
		
		int nButtons= buttonNames.length;
		fButtonsSelected= new boolean[nButtons];
		fButtonsEnabled= new boolean[nButtons];
		for (int i= 0; i < nButtons; i++) {
			fButtonsSelected[i]= false;
			fButtonsEnabled[i]= true;
		}
		if (fButtonsStyle == SWT.RADIO) {
			fButtonsSelected[0]= true;
		}
		
		fGroupBorderStyle= borderStyle;
		fGroupNumberOfColumns= (nColumns <= 0) ? nButtons : nColumns;
		
		fButtonsStyle= buttonsStyle;
		
	}
	
	// ------- layout helpers
		
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		assertEnoughColumns(nColumns);
				
		if (fGroupBorderStyle == -1) {
			Label label= getLabelControl(parent);
			label.setLayoutData(gridDataForLabel(1));
		
			Composite buttonsgroup= getSelectionButtonsGroup(parent);
			MGridData gd= new MGridData();
			gd.horizontalSpan= nColumns - 1;
			gd.grabColumn= 0;
			buttonsgroup.setLayoutData(gd);
			
			return new Control[] { label, buttonsgroup };
		} else {
			Composite buttonsgroup= getSelectionButtonsGroup(parent);
			MGridData gd= new MGridData();
			gd.horizontalSpan= nColumns;
			gd.grabColumn= 0;
			buttonsgroup.setLayoutData(gd);
		
			return new Control[] { buttonsgroup };
		}
	}	
	
	public int getNumberOfControls() {
		return (fGroupBorderStyle == -1) ? 2 : 1;
	}
	
	public void setButtonsMinWidth(int minWidth) {
		if (fButtonComposite != null) {
			Control[] control= fButtonComposite.getChildren();
			if (control != null && control.length > 0) {
				((MGridData)control[0].getLayoutData()).widthHint= minWidth;
			}
		}
	}	
	
	// ------- ui creation
	
	private Button createSelectionButton(int index, Composite group, SelectionListener listener) {
		Button button= new Button(group, fButtonsStyle | SWT.LEFT);
		button.setFont(group.getFont());			
		button.setText(fButtonNames[index]);
		button.setEnabled(isEnabled() && fButtonsEnabled[index]);
		button.setSelection(fButtonsSelected[index]);
		button.addSelectionListener(listener);
		button.setLayoutData(new MGridData());
		return button;
	}

	public Composite getSelectionButtonsGroup(Composite parent) {
		if (fButtonComposite == null) {
			assertCompositeNotNull(parent);
			
			MGridLayout layout= new MGridLayout();
			layout.makeColumnsEqualWidth= true;
			layout.numColumns= fGroupNumberOfColumns;			
			
			if (fGroupBorderStyle != -1) {
				Group group= new Group(parent, fGroupBorderStyle);
				if (fLabelText != null && !"".equals(fLabelText)) { //$NON-NLS-1$
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
		}
		return fButtonComposite;
	}
	
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
	
	// ------ model access	
		
	public boolean isSelected(int index) {
		if (index >= 0 && index < fButtonsSelected.length) {
			return fButtonsSelected[index];
		}
		return false;
	}
	
	public void setSelection(int index, boolean selected) {
		if (index >= 0 && index < fButtonsSelected.length) {
			if (fButtonsSelected[index] != selected) {
				fButtonsSelected[index]= selected;
				if (fButtons != null) {
					Button button= fButtons[index];
					if (isOkToUse(button)) {
						button.setSelection(selected);
					}
				}
			}
		}
	}

	// ------ enable / disable management
	
	protected void updateEnableState() {
		super.updateEnableState();
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
	
	public void enableSelectionButton(int index, boolean enable) {
		if (index >= 0 && index < fButtonsEnabled.length) {
			fButtonsEnabled[index]= enable;
			if (fButtons != null) {
				Button button= fButtons[index];
				if (isOkToUse(button)) {
					button.setEnabled(isEnabled() && enable);
				}
			}
		}
	}	
}