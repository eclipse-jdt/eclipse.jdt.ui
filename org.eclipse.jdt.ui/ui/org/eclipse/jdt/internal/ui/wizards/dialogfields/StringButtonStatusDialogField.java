/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.dialogfields;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jdt.internal.ui.wizards.swt.MGridData;

public class StringButtonStatusDialogField extends StringButtonDialogField {
		
	private Label fStatusLabelControl;
	private Object fStatus;  // String or ImageDescriptor
	
	private String fWidthHintString;
	private int fWidthHint;	
	
	public StringButtonStatusDialogField(IStringButtonAdapter adapter) {
		super(adapter);
		fStatus= null;
		fWidthHintString= null;
		fWidthHint= -1;
	}
	
	// ------ set status
	
	public void setStatus(String status) {
		if (isOkToUse(fStatusLabelControl)) {
			fStatusLabelControl.setText(status);
		}
		fStatus= status;		
	}
	
	/**
	 * Caller is responsible to dispose image
	 */
	public void setStatus(Image image) {
		if (isOkToUse(fStatusLabelControl)) {
			if (image == null) {
				fStatusLabelControl.setImage(null);
			} else {
				fStatusLabelControl.setImage(image);
			}
		}
		fStatus= image;		
	}	
		
	public void setStatusWidthHint(String widthHintString) {
		fWidthHintString= widthHintString;
		fWidthHint= -1;
	}
	
	public void setStatusWidthHint(int widthHint) {
		fWidthHint= widthHint;
		fWidthHintString= null;
	}
	
	// ------- layout helpers	
		
	public Control[] doFillIntoGrid(Composite parent, int nColumns) {
		assertEnoughColumns(nColumns);
		
		Label label= getLabelControl(parent);
		label.setLayoutData(gridDataForLabel(1));
		Text text= getTextControl(parent);
		text.setLayoutData(gridDataForText(nColumns - 3));
		Label status= getStatusLabelControl(parent);
		status.setLayoutData(gridDataForStatusLabel(parent, 1));
		Control button= getChangeControl(parent);
		button.setLayoutData(gridDataForControl(1));
		
		return new Control[] { label, text, status, button };
	}
	
	public int getNumberOfControls() {
		return 4;	
	}
	
	protected MGridData gridDataForStatusLabel(Control aControl, int span) {
		MGridData gd= new MGridData();
		gd.horizontalAlignment= gd.BEGINNING;
		gd.grabExcessHorizontalSpace= false;
		gd.horizontalIndent= 0;
		if (fWidthHintString != null) {
			GC gc= new GC(aControl);
			gd.widthHint= gc.textExtent(fWidthHintString).x;
			gc.dispose();
		} else if (fWidthHint != -1) {
			gd.widthHint= fWidthHint;
		} else {
			gd.widthHint= SWT.DEFAULT;
		}		
		return gd;
	}
	
	// ------- ui creation	
	
	public Label getStatusLabelControl(Composite parent) {
		if (fStatusLabelControl == null) {
			assertCompositeNotNull(parent);			
			fStatusLabelControl= new Label(parent, SWT.LEFT);
			fStatusLabelControl.setFont(parent.getFont());
			fStatusLabelControl.setEnabled(isEnabled());
			if (fStatus instanceof Image) {
				fStatusLabelControl.setImage((Image)fStatus);
			} else if (fStatus instanceof String) {
				fStatusLabelControl.setText((String)fStatus);
			} else {
				// must be null
			}
		}
		return fStatusLabelControl;
	}
	
	// ------ enable / disable management
	
	protected void updateEnableState() {
		super.updateEnableState();
		if (isOkToUse(fStatusLabelControl)) {
			fStatusLabelControl.setEnabled(isEnabled());
		}
	}	
}