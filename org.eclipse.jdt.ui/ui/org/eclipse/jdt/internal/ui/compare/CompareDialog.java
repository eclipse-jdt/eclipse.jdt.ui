/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import java.io.*;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.layout.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.compare.*;
import org.eclipse.compare.internal.*;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;


class CompareDialog extends Dialog {
	
	class ViewerSwitchingPane extends CompareViewerSwitchingPane {
		
		ViewerSwitchingPane(Composite parent, int style) {
			super(parent, style, false);
		}
	
		protected Viewer getViewer(Viewer oldViewer, Object input) {
			return CompareUIPlugin.findContentViewer(oldViewer, input, this, fCompareConfiguration);	
		}
		
//		public void setText(String label) {
//			super.setText(label);
//		}
		
		public void setImage(Image image) {
			// don't show icon
		}
	}
	
	private ResourceBundle fBundle;
	private CompareViewerSwitchingPane fContentPane;
	private CompareConfiguration fCompareConfiguration;
	private ICompareInput fInput;
	
	
	CompareDialog(Shell parent, ResourceBundle bundle) {
		super(parent);
		setShellStyle(SWT.CLOSE | SWT.APPLICATION_MODAL | SWT.RESIZE);
		
		fBundle= bundle;
		
		fCompareConfiguration= new CompareConfiguration();
		fCompareConfiguration.setLeftEditable(false);
		fCompareConfiguration.setRightEditable(false);
	}
	
	void compare(ICompareInput input) {
		
		fInput= input;
		
		fCompareConfiguration.setLeftLabel(fInput.getLeft().getName());
		fCompareConfiguration.setLeftImage(fInput.getLeft().getImage());
		
		fCompareConfiguration.setRightLabel(fInput.getRight().getName());
		fCompareConfiguration.setRightImage(fInput.getRight().getImage());
		
		if (fContentPane != null)
			fContentPane.setInput(fInput);
			
		open();
	}
	
	 /* (non Javadoc)
 	 * Creates SWT control tree.
 	 */
	protected synchronized Control createDialogArea(Composite parent) {
		
		getShell().setText(Utilities.getString(fBundle, "title")); //$NON-NLS-1$
		
		fContentPane= new ViewerSwitchingPane(parent, SWT.BORDER | SWT.FLAT);
		fContentPane.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
					| GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL));
		
		if (fInput != null)
			fContentPane.setInput(fInput);
			
		return fContentPane;
	}
	
	/* (non Javadoc)
	 * Returns the size initialized with the constructor.
	 */
	protected Point getInitialSize() {
		Point size= new Point(Utilities.getInteger(fBundle, "width", 0), //$NON-NLS-1$
					Utilities.getInteger(fBundle, "height", 0)); //$NON-NLS-1$
		
		Shell shell= getParentShell();
		if (shell != null) {
			Point parentSize= shell.getSize();
			if (size.x <= 0)
				size.x= parentSize.x-300;
			if (size.y <= 0)
				size.y= parentSize.y-200;
		}
		if (size.x < 700)
			size.x= 700;
		if (size.y < 500)
			size.y= 500;
		return size;
	}
	
	/* (non-Javadoc)
	 * Method declared on Dialog.
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		String buttonLabel= Utilities.getString(fBundle, "buttonLabel", IDialogConstants.OK_LABEL); //$NON-NLS-1$
		createButton(parent, IDialogConstants.CANCEL_ID, buttonLabel, false);
	}

}
