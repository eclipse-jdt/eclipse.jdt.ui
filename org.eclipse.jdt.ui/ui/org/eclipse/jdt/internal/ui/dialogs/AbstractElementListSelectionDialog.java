/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.viewers.ILabelProvider;

/**
 * A class to select one or more elements out of an indexed property
 */
public abstract class AbstractElementListSelectionDialog extends SelectionStatusDialog {
	
	private ILabelProvider fRenderer;
	private boolean fIgnoreCase= true;
	private boolean fIsMultipleSelection= false;
	
	private SelectionList fSelectionList;
	private Label fMessage;
	private ISelectionValidator fValidator;	
	
	private String fEmptyListMessage= ""; //$NON-NLS-1$
	private String fNothingSelectedMessage= ""; //$NON-NLS-1$
		
	private int fWidth= 60;
	private int fHeight= 18;

	/**
	 * Constructs a list selection dialog.
	 * @param renderer The label renderer used
	 * @param ignoreCase Decides if the match string ignores lower/upppr case
	 * @param multipleSelection Allow multiple selection	 
	 */
	protected AbstractElementListSelectionDialog(Shell parent,
		ILabelProvider renderer)
	{
		super(parent);
		
		fRenderer= renderer;
	}

	/**
	 * An element has been selected in the list by double clicking on it.
	 * Emulate a OK button pressed to close the dialog.
	 */	
	protected abstract void handleDoubleClick();
	
	public void setIgnoreCase(boolean ignoreCase) {
		fIgnoreCase= ignoreCase;
	}
	
	public void setMultipleSelection(boolean multipleSelection) {
		fIsMultipleSelection= multipleSelection;
	}
	
	/**
	 * If a empty-list message is set, a error message is shown
	 * Must be set before widget creation
	 */
	public void setEmptyListMessage(String message) {
		fEmptyListMessage= message;
	}

	/**
	 * If the selection is empty, this message is shown
	 */
	public void setNothingSelectedMessage(String message) {
		fNothingSelectedMessage= message;
	}
	
	/**
	 * A validator can be set to check if the current selection
	 * is valid
	 */
	public void setValidator(ISelectionValidator validator) {
		fValidator= validator;
	}	
	
	/**
	 * Initializes the selection list widget with the given list of
	 * elements.
	 */
	protected void setSelectionListElements(List elements, boolean refilter) {
		fSelectionList.setElements(elements, refilter);
	}

	/**
	 * Sets the filter text to the given value.
	 */
	protected void setFilter(String text, boolean refilter) {
		fSelectionList.setFilter(text, refilter);
	} 
	
	/**
	 * Returns the currently used filter text.
	 */
	protected String getFilter() {
		return fSelectionList.getFilter();
	}
	
	/**
	 * Refilters the current list according to the filter entered into the
	 * text edit field.
	 */
	protected void refilter() {
		fSelectionList.filter(true);
	}
	 
	/**
	 * Returns the selection indices.
	 */
	protected int[] getSelectionIndices() {
		return fSelectionList.getSelectionIndices();
	}
	
	/**
	 * Selects the elements in the list determined by the given
	 * selection indices.
	 */
	protected void setSelection(int[] selection) {
		fSelectionList.setSelection(selection);
	} 
	 	
	/**
	 * Returns the widget selection. Returns empty list when the widget is not
	 * usable.
	 */
	protected List getWidgetSelection() {
		if (fSelectionList == null || fSelectionList.isDisposed())
			return new ArrayList(0);
		return fSelectionList.getSelection();	
	}
	
	/*
	 * @private
	 */
	public int open() {
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				access$superOpen();
			}
		});
		return getReturnCode() ;
	}
	
	/*
	 * @private
	 */
	private void access$superOpen() {
		super.open();
	} 
	 
	/**
	 * Creates the message text widget and sets layout data.
	 */
	protected Label createMessageArea(Composite composite) {
		Label label= super.createMessageArea(composite);

		GridData data= new GridData();
		data.grabExcessVerticalSpace= false;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		label.setLayoutData(data);
		
		return label;
	}	

	/**
	 * Creates the selection list.
	 */
	private SelectionList createSelectionList(Composite parent) {		
		int flags= SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
			(fIsMultipleSelection ? SWT.MULTI : SWT.SINGLE);
			
		SelectionList list= new SelectionList(parent, flags, fRenderer, fIgnoreCase);		
		list.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				handleDoubleClick();
			}
			public void widgetSelected(SelectionEvent e) {
				verifyCurrentSelection();
			}
		});

		GridData data= new GridData();
		data.widthHint= convertWidthInCharsToPixels(fWidth);
		data.heightHint= convertHeightInCharsToPixels(fHeight);
		data.grabExcessVerticalSpace= true;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.FILL;
		list.setLayoutData(data);

		return list;
	}			
		
	/**
	 * Returns <code>true</code> if the list of elements is empty,
	 * <code>false</code> otherwise.
	 */
	protected boolean isEmptyList() {
		return (fSelectionList == null) || fSelectionList.isEmptyList();
	}
	
	/**
	 * Verifies the current selection and updates the status line
	 * accordingly.
	 */
	protected boolean verifyCurrentSelection() {
		IStatus status;
		List sel= getWidgetSelection();
		int length= sel.size();
		if (length > 0) {
			if (fValidator != null) {
				status= fValidator.validate(sel.toArray());
			} else {
				status= new StatusInfo();
			}
		} else {
			
			if (isEmptyList()) {
				status= new StatusInfo(IStatus.ERROR, fEmptyListMessage);
			} else {
				status= new StatusInfo(IStatus.ERROR, fNothingSelectedMessage);
			}
		}
		updateStatus(status);
		return status.isOK();
	}

	/*
	 * @private
	 * @see Dialog#cancelPressed
	 */
	protected void cancelPressed() {
		setResult(null);
		super.cancelPressed();
	}	

	/*
	 * @private
	 * @see Window#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite contents= (Composite) super.createDialogArea(parent);
		
		fMessage= createMessageArea(contents);
		fSelectionList= createSelectionList(contents);
		
		return contents;
	}

	/*
	 * @private
	 * @see Window#create(Shell)
	 */
	public void create() {
		super.create();

    	if (isEmptyList()) {
     		fMessage.setEnabled(false);
     		fSelectionList.setEnabled(false);
     	} else {
	     	verifyCurrentSelection();		
			fSelectionList.selectFilterText();
			fSelectionList.setFocus();
     	}	
	}
	
	/**
	 * Sets the size in unit of characters.
	 */
	public void setSize(int width, int height) {
		fWidth= width;
		fHeight= height;
	}
	
	/**
	 * Returns the size in unit of characters.
	 */
	public int[] getSize() {
		return new int[] {fWidth, fHeight};
	}
}