/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;

import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;

public class CheckedListDialogField extends ListDialogField {
	
	private int fCheckAllButtonIndex;
	private int fUncheckAllButtonIndex;
	
	private List fCheckElements;

	public CheckedListDialogField(IListAdapter adapter, String[] customButtonLabels, ILabelProvider lprovider) {
		super(adapter, customButtonLabels, lprovider);
		fCheckElements= new ArrayList();
		
		fCheckAllButtonIndex= -1;
		fUncheckAllButtonIndex= -1;
	}
	
	public void setCheckAllButtonIndex(int checkButtonIndex) {
		Assert.isTrue(checkButtonIndex < fButtonLabels.length);
		fCheckAllButtonIndex= checkButtonIndex;
	}
	
	public void setUncheckAllButtonIndex(int uncheckButtonIndex) {
		Assert.isTrue(uncheckButtonIndex < fButtonLabels.length);
		fUncheckAllButtonIndex= uncheckButtonIndex;
	}
	

	// hook to create the CheckboxTableViewer
	protected TableViewer createTableViewer(Composite parent) {
		Table table= new Table(parent, SWT.CHECK + getListStyle());
		CheckboxTableViewer tableViewer= new CheckboxTableViewer(table);
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent e) {
				doCheckStateChanged(e);
			}
		});
		return tableViewer;
	}		
	
	
	// hook to set the checked elements (can only be done after widget creation)
	public Control getListControl(Composite parent) {
		Control control= super.getListControl(parent);
		((CheckboxTableViewer)fTable).setCheckedElements(fCheckElements.toArray());
		return control;
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
	
			
	private void doCheckStateChanged(CheckStateChangedEvent e) {
		if (e.getChecked()) {
			fCheckElements.add(e.getElement());
		} else {
			fCheckElements.remove(e.getElement());
		}		
		checkStateChanged();
	}
	
	// ------ enable / disable management
	
	protected boolean getExtraButtonState(ISelection sel, int index) {
		if (index == fCheckAllButtonIndex) {
			return !fElements.isEmpty();
		} else if (index == fUncheckAllButtonIndex) {
			return !fElements.isEmpty();
		}
		return super.getExtraButtonState(sel, index);
	}	
	
	protected boolean extraButtonPressed(int index) {
		if (index == fCheckAllButtonIndex) {
			checkAll(true);
		} else if (index == fUncheckAllButtonIndex) {
			checkAll(false);
		} else {
			return super.extraButtonPressed(index);
		}
		return true;
	}
	
				
	
	

}