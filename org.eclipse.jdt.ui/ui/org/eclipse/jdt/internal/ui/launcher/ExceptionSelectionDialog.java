/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
 
package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.internal.ui.dialogs.ElementListSelectionDialog;

public class ExceptionSelectionDialog extends ElementListSelectionDialog {
	protected Button fCaughtBox;
	protected Button fUncaughtBox;
	
	protected boolean fCaught= true;
	protected boolean fUncaught= true;
	
	protected final static String PREFIX= "launcher.exception_dialog.";
	protected final static String CAUGHT= PREFIX + "caught";
	protected final static String UNCAUGHT= PREFIX + "uncaught";
	protected final static String TITLE= PREFIX + "title";
	protected final static String MESSAGE= PREFIX + "message";
	

	public ExceptionSelectionDialog(Shell parent, ILabelProvider renderer) {
		super(parent, JavaLaunchUtils.getResourceString(TITLE), null, renderer, true, true);
		setMessage(JavaLaunchUtils.getResourceString(MESSAGE));
	}
	
	/**
	 * @see ElementListSelectionDialog#createInnerControl
	 */
	protected Control createDialogArea(Composite parent) {
		Composite contents= (Composite)super.createDialogArea(parent);
		
		fCaughtBox= new Button(contents, SWT.CHECK);
		fCaughtBox.setText(JavaLaunchUtils.getResourceString(CAUGHT));
		fCaughtBox.setLayoutData(new GridData());
		fCaughtBox.setSelection(fCaught);
		fUncaughtBox= new Button(contents, SWT.CHECK);
		fUncaughtBox.setLayoutData(new GridData());
		fUncaughtBox.setText(JavaLaunchUtils.getResourceString(UNCAUGHT));
		fUncaughtBox.setSelection(fUncaught);
		
		if (isEmptyList()) {
			fCaughtBox.setEnabled(false);
			fUncaughtBox.setEnabled(false);
		}

		return contents;
	}
	
	/*
	 * @private
	 */
	protected void okPressed() {
		fCaught= fCaughtBox.getSelection();
		fUncaught= fUncaughtBox.getSelection();
		super.okPressed();
	}	

	public boolean isCaughtEnabled() {
		return fCaught;
	}
	
	public boolean isUncaughtEnabled() {
		return fUncaught;
	}
	
	protected Point computeInitialSize() {
		Point size= super.computeInitialSize();
		size.x= convertWidthInCharsToPixels(60);
		return size;
	}
}