/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;


public class VisibilityWorkbookPage extends BuildPathBasePage {
			
	private ListDialogField fClassPathList;
	private IJavaProject fCurrJProject;
	
	private CheckedListDialogField fExportList;
	
	public VisibilityWorkbookPage(ListDialogField classPathList) {
		fClassPathList= classPathList;
				
		VisibilityListListener listener= new VisibilityListListener();
		
		String[] buttonLabels= new String[] {
			/* 0 */ NewWizardMessages.getString("VisibilityWorkbookPage.visibility.checkall.button"), //$NON-NLS-1$
			/* 1 */ NewWizardMessages.getString("VisibilityWorkbookPage.visibility.uncheckall.button") //$NON-NLS-1$
		};
		
		fExportList= new CheckedListDialogField(null, buttonLabels, new CPListLabelProvider());
		fExportList.setDialogFieldListener(listener);
		fExportList.setLabelText(NewWizardMessages.getString("VisibilityWorkbookPage.visibility.label")); //$NON-NLS-1$
		fExportList.setCheckAllButtonIndex(0);
		fExportList.setUncheckAllButtonIndex(1);
	}
	
	public void update() {
		List cpelements= fClassPathList.getElements();
		List exportableElements= new ArrayList(cpelements.size());
		List checkedElements= new ArrayList(cpelements.size());
		
		int nElements= cpelements.size();
		for (int i= 0; i < nElements; i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			int kind= cpe.getEntryKind();
			if (kind == IClasspathEntry.CPE_LIBRARY || kind == IClasspathEntry.CPE_VARIABLE || kind == IClasspathEntry.CPE_PROJECT) {
				exportableElements.add(cpe);
				if (cpe.isExported()) {
					checkedElements.add(cpe);
				}
			}
		}
		fExportList.setElements(exportableElements);
		fExportList.setCheckedElements(checkedElements);
	}		
		
	// -------- UI creation ---------
		
	public Control getControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
			
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fExportList }, true, 0, 0, SWT.DEFAULT, SWT.DEFAULT);
		
		fExportList.setButtonsMinWidth(110);
		fExportList.getTableViewer().setSorter(new CPListElementSorter());	
				
		return composite;
	}
	
	private class VisibilityListListener implements IDialogFieldListener {
			
		// ---------- IDialogFieldListener --------
	
		public void dialogFieldChanged(DialogField field) {
			updateVisibilityList();
		}
	}
	
	private void updateVisibilityList() {
		List cpelements= fExportList.getElements();
		for (int i= 0; i < cpelements.size(); i++) {
			CPListElement cpe= (CPListElement)cpelements.get(i);
			cpe.setExported(fExportList.isChecked(cpelements.get(i)));
		}
	}
	

	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fExportList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		for (int i= selElements.size()-1; i >= 0; i--) {
			CPListElement curr= (CPListElement) selElements.get(i);
			int kind= curr.getEntryKind();
			if (kind == IClasspathEntry.CPE_SOURCE) {
				selElements.remove(i);
			}
		}
		fExportList.selectElements(new StructuredSelection(selElements));
	}		


}