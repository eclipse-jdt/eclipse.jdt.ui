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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.swt.widgets.Text;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ILabelProvider;

/**
 * A class to select one or more elements out of an indexed property
 */
public abstract class AbstractElementListSelectionDialog extends SelectionStatusDialog {
	
	private ILabelProvider fRenderer;
	private boolean fIgnoreCase= true;
	private boolean fIsMultipleSelection= false;
	private boolean fMatchEmptyString= true;
	private boolean fAllowDuplicates= true;
	
	private Label fMessage;

	protected FilteredList fFilteredList;
	private Text fFilterText;
	
	private ISelectionValidator fValidator;	
	private String fFilter= ""; //$NON-NLS-1$
	
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
	protected AbstractElementListSelectionDialog(Shell parent, ILabelProvider renderer)
	{
		super(parent);		
		fRenderer= renderer;
	}

	/**
	 * An element in the list has been default selected (double click).
	 * Emulate a OK button pressed to close the dialog.
	 */	
	protected void handleDefaultSelected() {
		if (verifyCurrentSelection())
			buttonPressed(IDialogConstants.OK_ID);
	}
	
	public void setIgnoreCase(boolean ignoreCase) {
		fIgnoreCase= ignoreCase;
	}
	
	public boolean isCaseIgnored() {
		return fIgnoreCase;
	}
	
	public void setMatchEmptyString(boolean matchEmptyString) {
		fMatchEmptyString= matchEmptyString;
	}
	
	public void setMultipleSelection(boolean multipleSelection) {
		fIsMultipleSelection= multipleSelection;
	}

	public void setAllowDuplicates(boolean allowDuplicates) {
		fAllowDuplicates= allowDuplicates;
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
	 * elements. To be called within open().
	 */
	protected void setSelectionListElements(List elements) {
		Assert.isNotNull(fFilteredList);
		fFilteredList.setElements(elements);
	}

	/**
	 * Sets the filter text to the given value.
	 */
	protected void setFilter(String text) {
		if (fFilterText == null)
			fFilter= text;
		else
			fFilterText.setText(text);
	} 
	
	/**
	 * Returns the currently used filter text.
	 */
	protected String getFilter() {
		if (fFilteredList == null)
			return fFilter;
		else
			return fFilteredList.getFilter();
	}

	/**
	 * Returns the selection indices.
	 * To be called within or after open().
	 */
	protected int[] getSelectionIndices() {
		Assert.isNotNull(fFilteredList);
		return fFilteredList.getSelectionIndices();
	}
	
	/**
	 * Selects the elements in the list determined by the given
	 * selection indices.
	 * To be called within open().
	 */
	protected void setSelection(int[] selection) {
		Assert.isNotNull(fFilteredList);
		fFilteredList.setSelection(selection);
	} 
	 	
	/**
	 * Returns the widget selection. Returns empty list when the widget is not
	 * usable.
	 * To be called within or after open().
	 */
	protected List getWidgetSelection() {
		if (fFilteredList == null || fFilteredList.isDisposed())
			return new ArrayList(0);
		return fFilteredList.getSelection();	
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
		return getReturnCode();
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
		
		fMessage= label;
		
		return label;
	}	
	
	/**
	 * Returns <code>true</code> if the list of elements is empty,
	 * <code>false</code> otherwise.
	 */
	protected boolean isEmptyList() {
		return (fFilteredList == null) || fFilteredList.isEmpty();
	}
	
	protected void handleSelectionChanged() {
		verifyCurrentSelection();
	}
	
	/**
	 * Verifies the current selection and updates the status line
	 * accordingly.
	 */
	protected boolean verifyCurrentSelection() {
		IStatus status;
		List selection= getWidgetSelection();

		if (selection.size() > 0) {
			if (fValidator != null) {
				status= fValidator.validate(selection.toArray());
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

	protected FilteredList createFilteredList(Composite parent) {
		int flags= SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL |
			(fIsMultipleSelection ? SWT.MULTI : SWT.SINGLE);
			
		FilteredList list= new FilteredList(parent, flags, fRenderer,
			fIgnoreCase, fAllowDuplicates, fMatchEmptyString);

		GridData data= new GridData();
		data.widthHint= convertWidthInCharsToPixels(fWidth);
		data.heightHint= convertHeightInCharsToPixels(fHeight);
		data.grabExcessVerticalSpace= true;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.FILL;
		list.setLayoutData(data);

		list.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
				handleDefaultSelected();
			}
			public void widgetSelected(SelectionEvent e) {
				handleSelectionChanged();
			}
		});

		fFilteredList= list;		

		return list;		
	}
	
	protected void initFilteredList() {
		fFilteredList.setFilter(fFilter);				
	}
	
	protected Text createFilterText(Composite parent) {
		Text text= new Text(parent, SWT.BORDER);

		GridData data= new GridData();
		data.grabExcessVerticalSpace= false;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		text.setLayoutData(data);
		
		Listener listener= new Listener() {
			public void handleEvent(Event e) {
				fFilteredList.setFilter(fFilterText.getText());
			}
		};		
		text.addListener(SWT.Modify, listener);

		fFilterText= text;
				
		return text;
	}

	protected void initFilterText() {
		fFilterText.setText(fFilter);
	}
	
	/*
	 * @private
	 * @see Window#create(Shell)
	 */
	public void create() {
		super.create();

		Assert.isNotNull(fFilteredList);

    	if (isEmptyList()) {
     		fMessage.setEnabled(false);
     		fFilterText.setEnabled(false);
     		fFilteredList.setEnabled(false);
     	} else {
	     	verifyCurrentSelection();		
			fFilterText.selectAll();
			fFilterText.setFocus();
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