/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import org.eclipse.jface.viewers.StructuredSelection;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.swt.MGridLayout;


public class ClasspathOrderingWorkbookPage extends BuildPathBasePage {
	
	private ListDialogField fClassPathList;
	
	public ClasspathOrderingWorkbookPage(ListDialogField classPathList) {
		fClassPathList= classPathList;
	}
	
	public Control getControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		
		DialogField[] editors= new DialogField[] { fClassPathList };
		LayoutUtil.doDefaultLayout(composite, editors, true);
		
		MGridLayout layout= (MGridLayout)composite.getLayout();
		layout.marginWidth= 5;
		layout.marginHeight= 5;
		
		fClassPathList.setButtonsMinWidth(110);
				
		return composite;
	}
	
	/**
	 * @see BuildPathBasePage#getSelection
	 */
	public List getSelection() {
		return fClassPathList.getSelectedElements();
	}

	/**
	 * @see BuildPathBasePage#setSelection
	 */	
	public void setSelection(List selElements) {
		fClassPathList.selectElements(new StructuredSelection(selElements));
	}		

}