/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.text.MessageFormat;import java.util.ArrayList;import java.util.Arrays;import java.util.List;import org.eclipse.swt.SWT;import org.eclipse.swt.graphics.Image;import org.eclipse.swt.layout.GridData;import org.eclipse.swt.layout.GridLayout;import org.eclipse.swt.widgets.Button;import org.eclipse.swt.widgets.Composite;import org.eclipse.swt.widgets.Control;import org.eclipse.swt.widgets.Label;import org.eclipse.swt.widgets.Shell;import org.eclipse.core.runtime.IStatus;import org.eclipse.jface.dialogs.IDialogConstants;import org.eclipse.jface.util.Assert;import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jdt.internal.ui.JavaUIMessages;

public class MultiElementListSelectionDialog extends AbstractElementListSelectionDialog {
		
	private Object[][] fElements;
	
	private boolean[] fPagesOKStates;
	
	private int[][] fSelectedIndices;
	
	private int fCurrentPage;
	private int fNumberOfPages;
	
	private Button fFinishButton;
	private Button fBackButton;
	private Button fNextButton;
	
	private Label fPageInfoLabel;
	
	private String fPageInfoMessage;
	
	/**
	 * Constructs a list selection dialog.
	 * @param renderer The label renderer used
	 * @param ignoreCase Decides if the match string ignores lower/upppr case
	 * @param multipleSelection Allow multiple selection	 
	 */
	public MultiElementListSelectionDialog(Shell parent, ILabelProvider renderer) {
		super(parent, renderer);
		fPageInfoMessage= JavaUIMessages.getString("MultiElementListSelectionDialog.pageInfoMessage"); //$NON-NLS-1$
	}
	
	/**
	 * Sets message shown in right top corner. Use {0} and {1} as placeholders
	 * for the current and the total number of pages.
	 */
	public void setPageInfoMessage(String message) {
		fPageInfoMessage= message;
	}
	
	/**
	 * Sets the elements to be shown in the dialog.
	 */
	public void setElements(Object[][] elements) {
		fElements= elements;
		fNumberOfPages= elements.length;			
		fPagesOKStates= new boolean[fNumberOfPages]; // all initialized with false
		fSelectedIndices= new int[fNumberOfPages][]; // all initialized with null
		initializeResult(fNumberOfPages);
	}
	 
	/**
	 * Returns the arrays of selected elements after the dialog was shown
	 * If cancel was pressed, returns <code>null</code>
	 * @deprecated Use getResult instead.
	 */
	public Object[][] getAllSelectedElements() {
		Object[] result= getResult();
		if (result == null || result.length == 0)
			return null;
			
		Object[][] r= new Object[result.length][];
		for (int i= 0; i < r.length; i++) {
			List l= (List)result[i];
			if (l != null)
				r[i]= l.toArray();
		}
		return r;
	}

	/**
	 * Returns the (single) selected elements after the dialog was shown
	 * If cancel was pressed, returns null
	 * @deprecated Use getResult instead.
	 */	
	public Object[] getSelectedElements() {
		Object[] result= getResult();
		
		if (result == null || result.length == 0)
			return null;	 		
		Object[] res= new Object[result.length];
		for (int i= 0; i < res.length; i++) {
			List l= (List)result[i];
			if (l != null && l.size() > 0) {
				res[i]= l.get(0);
			} else {
				res[i]= null;
			}
		}
		return res;
	}
	
	/**
	 * Returns a selected element after the dialog was shown
	 * If cancel was pressed, returns null
	 * @deprecated Use getResult instead.
	 */	
	public Object getSelectedElement() {
		Object[] result= getResult();
		if (result == null || result.length == 0)
			return null;
			
		List l= (List)result[0];
		if (l == null || l.size() == 0)
			return null;
		return l.get(0);		
	}		

	//---- Widget creation -----------------------------------------------------------------

	/**
	 * @private
	 */	
	protected Control createDialogArea(Composite parent) {
		Control result= super.createDialogArea(parent);
		fCurrentPage= 0;
		setPageData();		
		return result;
	}

	/**
	 * @private
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		fBackButton= createButton(parent, IDialogConstants.BACK_ID, IDialogConstants.BACK_LABEL, false);
		fNextButton= createButton(parent, IDialogConstants.NEXT_ID, IDialogConstants.NEXT_LABEL, true);
		fFinishButton= createButton(parent, IDialogConstants.OK_ID, IDialogConstants.FINISH_LABEL, false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	/**
	 * @private
	 */
	protected Label createMessageArea(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);

		GridLayout layout= new GridLayout();
		layout.marginHeight= 0;
		layout.marginWidth= 0;
		layout.horizontalSpacing= 5;
		layout.numColumns= 2;
		composite.setLayout(layout);
		
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		composite.setLayoutData(data);
		
		Label messageLabel= super.createMessageArea(composite);
		
		fPageInfoLabel= new Label(composite, SWT.NULL);
		fPageInfoLabel.setText(getPageInfoMessage());
		
		data= new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalAlignment= data.END;
		fPageInfoLabel.setLayoutData(data);
		
		return messageLabel;
	}
	
	//---- User input handling -------------------------------------------------------------
	
	/**
	 * @private
	 */
	protected void computeResult() {
		setResult(fCurrentPage, getWidgetSelection());
	}
		
	/**
	 * @private
	 */
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.BACK_ID) {
			turnPage(false);
		} else if (buttonId == IDialogConstants.NEXT_ID) {
			turnPage(true);
		} else {
			super.buttonPressed(buttonId);
		}			
	}
	
	/**
	 *
	 */
	protected void handleDefaultSelected() {
		if (verifyCurrentSelection()) {
			if (fCurrentPage == fNumberOfPages - 1) {
				buttonPressed(IDialogConstants.OK_ID);
			} else {
				buttonPressed(IDialogConstants.NEXT_ID);
			}
		}
	}
	
	/**
	 * @private
	 * @see AbstractElementListSelectionDialog#updateButtonsEnableState
	 */
	protected void updateButtonsEnableState(IStatus status) {
		boolean isOK= !status.matches(IStatus.ERROR);
		fPagesOKStates[fCurrentPage]= isOK;

		fNextButton.setEnabled(isOK && (fCurrentPage < fNumberOfPages - 1));
		fBackButton.setEnabled(fCurrentPage != 0);
		
		boolean isAllOK= isOK;
		int i= 0;
		while (isAllOK && i < fNumberOfPages) {
			isAllOK &= fPagesOKStates[i++];
		}
		
		fFinishButton.setEnabled(isAllOK);
	}
	
	private void turnPage(boolean toNextPage) {
		setResult(fCurrentPage, getWidgetSelection());
		setInitialSelection(fCurrentPage, getFilter());
		fSelectedIndices[fCurrentPage]= getSelectionIndices();
		
		if (toNextPage) {
			if (fCurrentPage < fNumberOfPages - 1) {
				fCurrentPage++;
			} else {
				return;
			}
		} else {
			if (fCurrentPage > 0) {
				fCurrentPage--;
			} else {
				return;
			}
		}
		
		if (fPageInfoLabel != null && !fPageInfoLabel.isDisposed())
			fPageInfoLabel.setText(getPageInfoMessage());
		
		setPageData();		
		
		verifyCurrentSelection();
	}
	
	//---- Private Helpers ------------------------------------------------------------
	
	private void setPageData() {
		setSelectionListElements(Arrays.asList(fElements[fCurrentPage]), false);
		String initSelection= (String)getInitialSelections().get(fCurrentPage);
	
		if (initSelection != null)
			setFilter(initSelection, true);
		else
			refilter();
			
		int[] selectedIndex= fSelectedIndices[fCurrentPage];
		if (selectedIndex != null)
			setSelection(selectedIndex);
	}
	
	private String getPageInfoMessage() {
		if (fPageInfoMessage == null)
			return ""; //$NON-NLS-1$
			
		String[] args= new String[] { Integer.toString(fCurrentPage + 1), Integer.toString(fNumberOfPages) };	
		return MessageFormat.format(fPageInfoMessage, args);
	}
		
	private void initializeResult(int length) {
		List result= new ArrayList(length);
		for (int i= 0; i != length; i++)
			result.add(null);

		setResult(result);
	}
	
}