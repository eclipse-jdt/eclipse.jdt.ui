/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;


public class ClasspathOrderingWorkbookPage extends BuildPathBasePage {
	
	private ListDialogField fClassPathList;
	
	public ClasspathOrderingWorkbookPage(ListDialogField classPathList) {
		fClassPathList= classPathList;
	}
	
	public Control getControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		LayoutUtil.doDefaultLayout(composite, new DialogField[] { fClassPathList }, true, 0, 0, SWT.DEFAULT, SWT.DEFAULT);

		int buttonBarWidth= SWTUtil.convertWidthInCharsToPixels(24, composite);
		fClassPathList.setButtonsMinWidth(buttonBarWidth);
			
		return composite;
	}
	
	/*
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fClassPathList.getSelectedElements();
	}

	/*
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		fClassPathList.selectElements(new StructuredSelection(selElements));
	}		

}