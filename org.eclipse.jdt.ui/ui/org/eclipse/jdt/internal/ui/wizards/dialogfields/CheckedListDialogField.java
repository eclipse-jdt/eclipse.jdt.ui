/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TableViewer;

public class CheckedListDialogField extends ListDialogField {
	
	private Button fCheckAllButton;
	private Button fUncheckAllButton;
	private String fCheckAllButtonLabel;
	private String fUncheckAllButtonLabel;
	
	private List fCheckElements;
	
	/**
	 * Create a table without custom  / remove buttons
	 */		
	public CheckedListDialogField(ILabelProvider lprovider, int config) {
		super(lprovider, config);
		fCheckElements= new ArrayList();
		fCheckAllButtonLabel= "!Check All!";
		fUncheckAllButtonLabel= "!Uncheck All!";		
	}
	
	public CheckedListDialogField(IListAdapter adapter, String[] customButtonLabels, ILabelProvider lprovider, int config) {
		super(adapter, customButtonLabels, lprovider, config);
		fCheckElements= new ArrayList();
		fCheckAllButtonLabel= "!Check All!";
		fUncheckAllButtonLabel= "!Uncheck All!";		
	}
	
	public void setCheckAllButtonLabel(String checkButtonLabel) {
		fCheckAllButtonLabel= checkButtonLabel;
	}
	
	public void setUncheckAllButtonLabel(String uncheckButtonLabel) {
		fUncheckAllButtonLabel= uncheckButtonLabel;
	}
	

	// hook to create the CheckboxTableViewer
	protected TableViewer createTableViewer(Composite parent) {
		Table table= new Table(parent, SWT.CHECK + getListStyle());
		CheckboxTableViewer tableViewer= new CheckboxTableViewer(table);
		tableViewer.addCheckStateListener(new CheckListener());
		return tableViewer;
	}		
	
	
	// hook to set the checked elements (can only be done after widget creation)
	public Control getListControl(Composite parent) {
		Control control= super.getListControl(parent);
		((CheckboxTableViewer)fTable).setCheckedElements(fCheckElements.toArray());
		return control;
	}	
	
	// hook to add some own buttons
	protected void createExtraButtons(Composite parent) {
		SelectionListener listener= new CheckListener();
		
		fCheckAllButton= createButton(parent, fCheckAllButtonLabel, listener);
		fUncheckAllButton= createButton(parent, fUncheckAllButtonLabel, listener);
	}
	
	// hook in to get element changes to update check model
	public void dialogFieldChanged() {
		for (int i= fCheckElements.size() -1; i >= 0; i--) {
			if (!fElements.contains(fCheckElements.get(i))) {
				fCheckElements.remove(i);
			}
		}
		super.dialogFieldChanged();
	}	
	
	private void checkStateChanged() {
		//call super and do not update check model
		super.dialogFieldChanged();
	}		
	
	// ------ enable / disable management
	
	private void updateCheckButtonState() {
		if (fTable != null) {
			boolean enabled= !fElements.isEmpty() && isEnabled();
			if (isOkToUse(fCheckAllButton)) {
				fCheckAllButton.setEnabled(enabled);
			}
			if (isOkToUse(fUncheckAllButton)) {
				fUncheckAllButton.setEnabled(enabled);
			}
		}
	}		
	
	
	/**
	 * @see ListDialogField#updateButtonState
	 */
	protected void updateButtonState() {
		super.updateButtonState();
		updateCheckButtonState();
	}
	
	// ------ model access
	
	
	public List getCheckedElements() {
		return new ArrayList(fCheckElements);
	}
	
	
	public void setCheckedElements(List list) {
		fCheckElements= list;
		if (fTable != null) {
			((CheckboxTableViewer)fTable).setCheckedElements(list.toArray());
		}
		checkStateChanged();
	}
	
	public void setChecked(Object object, boolean state) {
		if (!fCheckElements.contains(object)) {
			fCheckElements.add(object);
		}
		if (fTable != null) {
			((CheckboxTableViewer)fTable).setChecked(object, state);
		}
		checkStateChanged();
	}

	public void checkAll(boolean state) {
		if (state) {
			fCheckElements= getElements();
		} else {
			fCheckElements.clear();
		}
		if (fTable != null) {
			((CheckboxTableViewer)fTable).setAllChecked(state);
		}
		checkStateChanged();
	}
	
		
	// ------- CheckListener
	
	private class CheckListener implements SelectionListener, ICheckStateListener {
		
		// ------- SelectionListener
		
		public void widgetDefaultSelected(SelectionEvent e) {
			doCheckButtonPressed(e);
		}
		public void widgetSelected(SelectionEvent e) {
			doCheckButtonPressed(e);
		}		
		
		// ------- ICheckStateListener

		public void checkStateChanged(CheckStateChangedEvent e) {
			
			doCheckStateChanged(e);
		}
	}
	
	private void doCheckStateChanged(CheckStateChangedEvent e) {
		if (e.getChecked()) {
			fCheckElements.add(e.getElement());
		} else {
			fCheckElements.remove(e.getElement());
		}		
		checkStateChanged();
	}
	
	private void doCheckButtonPressed(SelectionEvent e) {
		if (e.widget == fCheckAllButton) {
			checkAll(true);
			return;
		} else if (e.widget == fUncheckAllButton) {
			checkAll(false);
			return;
		}
	}
	
				
	
	

}