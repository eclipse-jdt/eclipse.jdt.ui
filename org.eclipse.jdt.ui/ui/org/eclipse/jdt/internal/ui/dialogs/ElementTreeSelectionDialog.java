/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;

import org.eclipse.jdt.internal.ui.JavaUIMessages;
import org.eclipse.jdt.internal.ui.util.SelectionUtil;

public class ElementTreeSelectionDialog extends SelectionStatusDialog {
	
	private TreeViewer fViewer;
	private ILabelProvider fLabelProvider;
	private ITreeContentProvider fContentProvider;
	
	private ISelectionValidator fValidator= null;
	private ViewerSorter fSorter;
	private boolean fAllowMultiple= true;
	private boolean fDoubleClickSelects= true;
	private String fEmptyListMessage= JavaUIMessages.getString("ElementTreeSelectionDialog.nothing_available"); //$NON-NLS-1$	
	private int fInitialCharWidth= 40;
	private int fInitialCharHeight= 18;
	
	private IStatus fCurrStatus= new StatusInfo();
	private List fFilters;
	private Object fInput;		
	private boolean fIsEmpty;

	/**
	 * Constructor for the ElementTreeSelectionDialog.
	 * @param labelProvider The label provider to render the entries
	 * @param contentProvider The content provider to evaluate the tree structure
	 */	
	public ElementTreeSelectionDialog(Shell parent, ILabelProvider labelProvider,
		ITreeContentProvider contentProvider)
	{
		super(parent);
		
		fLabelProvider= labelProvider;
		fContentProvider= contentProvider;

		setResult(new ArrayList(0));
		setStatusLineAboveButtons(true);
	}	
	
	/**
	 * This message is shown when the tree has no entries at all
	 * Must be set before widget creation
	 */
	public void setEmptyListMessage(String message) {
		fEmptyListMessage= message;
	}	

	/**
	 *
	 * @param allowMultiple Specify the selection behaviour of the tree widget. Allows multiple selection or not 
	 */
	public void setAllowMultiple(boolean allowMultiple) {
		fAllowMultiple= allowMultiple;
	}
	
	/**
	 * Sets the double click selects flag.
	 */
	public void setDoubleClickSelects(boolean doubleClickSelects) {
		fDoubleClickSelects= doubleClickSelects;
	}
	
	/**
	 * Sets the sorter used by the tree viewer.
	 */
	public void setSorter(ViewerSorter sorter) {
		fSorter= sorter;
	}		
	
	/**
	 * Adds the given filter to the tree viewer.
	 */
	public void addFilter(ViewerFilter filter) {
		if (fFilters == null)
			fFilters= new ArrayList(4);
		fFilters.add(filter);
	}
	
	/**
	 * A validator can be set to check if the current selection
	 * is valid
	 */
	public void setValidator(ISelectionValidator validator) {
		fValidator= validator;
	}
	
	/**
	 * Sets the dialog's input to the given value.
	 * @param input the dialog's input.
	 */
	public void setInput(Object input) {
		fInput= input;
	} 

	/**
	 * @deprecated Use SelectionDialog.getResult() instead.
	 */
	public Object[] getSelectedElements() {
		return getResult();
	}
	
	/**
	 * Returns the selected element. If no element is selected or more
	 * than one element is selected then <code>null</code> is returned.
	 */
	public Object getSelectedElement() {
		Object[] result= getResult();
		if (result != null && result.length > 0)
			return result[0];
		return null;
	}
			
	/*
	 * @private
	 */	 
	protected void updateOKStatus() {
		if (!fIsEmpty) {
			if (fValidator != null) {
				fCurrStatus= fValidator.validate(getResult());
				updateStatus(fCurrStatus);
			} else {
				fCurrStatus= new StatusInfo();
			}
		} else {
			fCurrStatus= new StatusInfo(IStatus.ERROR, fEmptyListMessage);
		}
		updateStatus(fCurrStatus);
	}
	
	/*
	 * @private
	 */	 
	public int open() {
		fIsEmpty= evaluateIfTreeEmpty(fInput);
		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {
				access$superOpen();
			}
		});
		
		return getReturnCode();
	}
	
	public void setInitialSizeInCharacters(int width, int height) {
		fInitialCharWidth= width;
		fInitialCharHeight= height;
	}
	
	private void access$superOpen() {
		super.open();
	}	
	 
	/*
	 * @private
	 */	 
	protected void cancelPressed() {
		setResult(null);
		super.cancelPressed();
	} 

	/*
	 * @private
	 */	 
	protected void computeResult() {
		setResult(SelectionUtil.toList(fViewer.getSelection()));
	} 
	 
	/*
	 * @private
	 */	 
	public void create() {
		super.create();
		List initialSelections= getInitialSelections();
		if (initialSelections != null)
			fViewer.setSelection(new StructuredSelection(initialSelections), true);
		updateOKStatus();
	}		
	
	/*
	 * @private
	 */	 
	protected Control createDialogArea(Composite parent) {
		Composite composite= (Composite)super.createDialogArea(parent);
		
		Label messageLabel= createMessageArea(composite);
		
		Control treeWidget= createTreeViewer(composite);

		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= convertWidthInCharsToPixels(fInitialCharWidth);
		gd.heightHint= convertHeightInCharsToPixels(fInitialCharHeight);
		treeWidget.setLayoutData(gd);
		
		if (fIsEmpty) {
			messageLabel.setEnabled(false);
			treeWidget.setEnabled(false);
		}
		
		return composite;
	}
	
	private Tree createTreeViewer(Composite parent) {
		int selectionBehaviour= fAllowMultiple ? SWT.MULTI : SWT.SINGLE;
		fViewer= new TreeViewer(new Tree(parent, selectionBehaviour | SWT.BORDER));
		fViewer.setContentProvider(fContentProvider);
		fViewer.setLabelProvider(fLabelProvider);
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				access$setResult(SelectionUtil.toList(event.getSelection()));
				updateOKStatus();
			}	
		});
		fViewer.setSorter(fSorter);
		if (fFilters != null) {
			for (int i= 0; i < fFilters.size(); i++) {
				fViewer.addFilter((ViewerFilter)fFilters.get(i));
			}
		}
		
		if (fDoubleClickSelects) {
			Tree tree= fViewer.getTree();
			tree.addSelectionListener(new SelectionAdapter() {
				public void widgetDefaultSelected(SelectionEvent e) {
					updateOKStatus();
					if (fCurrStatus.isOK()) {
						access$superButtonPressed(IDialogConstants.OK_ID);
					}
				}
			});
		}
		
		fViewer.setInput(fInput);
		
		return fViewer.getTree();	
	}
	
	private boolean evaluateIfTreeEmpty(Object input) {
		Object[] elements= fContentProvider.getElements(input);
		if (elements.length > 0) {
			if (fFilters != null) {
				for (int i= 0; i < fFilters.size(); i++) {
					ViewerFilter curr= (ViewerFilter)fFilters.get(i);
					elements= curr.filter(fViewer, input, elements);
				}
			}
		}
		return elements.length == 0;
	}
	
	protected void access$superButtonPressed(int id) {
		super.buttonPressed(id);
	}
	
	protected void access$setResult(List result) {
		super.setResult(result);
	}		
}