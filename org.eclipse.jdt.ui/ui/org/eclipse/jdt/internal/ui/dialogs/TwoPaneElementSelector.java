/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.jdt.internal.core.refactoring.util.Selection;

public class TwoPaneElementSelector extends AbstractElementListSelectionDialog {

	private String fUpperListLabel;
	private String fLowerListLabel;
	private Table fLowerList;
	private ILabelProvider fQualifierRenderer;

	private Object[] fElements= new Object[0];
	private Object[] fQualifierElements;
		
	/**
	 * Creates the two pane element selector.
	 */
	public TwoPaneElementSelector(Shell parent, ILabelProvider elementRenderer, 
		ILabelProvider qualifierRenderer)
	{
		super(parent, elementRenderer);

		setSize(50, 15);
		setAllowDuplicates(false);
		
		fQualifierRenderer= qualifierRenderer;	
	}				

	public void setUpperListLabel(String label) {
		fUpperListLabel= label;
	}

	public void setLowerListLabel(String label) {
		fLowerListLabel= label;
	}	
	
	public void setElements(Object[] elements) {
		fElements= elements;
	}

	/**
	 * @deprecated Use getPrimaryResult instead.
	 */
	public Object getSelectedElement() {
		Object[] result= getResult();
		if (result == null || result.length == 0)
			return null;
			
		return result[0];
	}

	/**
	 * getUIComponent method comment.
	 */
	public Control createDialogArea(Composite parent) {
		Composite contents= (Composite) super.createDialogArea(parent);

		createMessageArea(contents);
		createFilterText(contents);
		createLabel(contents, fUpperListLabel);
		createFilteredList(contents);
		createLabel(contents, fLowerListLabel);
		createLowerList(contents);	

		initFilteredList();
		initFilterText();		
//		setSelectionListElements(Arrays.asList(fElements));
		setSelectionListElements(fElements);
		
		return contents;
	}

	private Label createLabel(Composite parent, String name) {
		Label label= new Label(parent, SWT.NONE);
		label.setText(name);
		
		return label;
	}
	
	/**
	 * Creates the list widget and sets layout data.
	 * @return org.eclipse.swt.widgets.List
	 */
	private Table createLowerList(Composite parent) {
		Table list= new Table(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		
		list.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event evt) {
				handleLowerSelectionChanged();
			}
		});
		
		list.addListener(SWT.MouseDoubleClick, new Listener() {
			public void handleEvent(Event evt) {
				handleDefaultSelected();
			}
		});
		
		list.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				fQualifierRenderer.dispose();
			}
		});
		
		GridData data= new GridData();
		data.widthHint= convertWidthInCharsToPixels(50);
		data.heightHint= convertHeightInCharsToPixels(5);
		data.grabExcessVerticalSpace= true;
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.FILL;
		list.setLayoutData(data);
		
		fLowerList= list;
		
		return list;
	}	

	/**
	 * @private
	 */
	protected void computeResult() {
		List result= new ArrayList(1);
		result.add(getWidgetSelection());
		setResult(result);
	}

	protected void handleDefaultSelected() {
		if (verifyCurrentSelection() && (getWidgetSelection2() != null))
			buttonPressed(IDialogConstants.OK_ID);
	}

	protected void handleSelectionChanged() {
		super.handleSelectionChanged();
		handleUpperSelectionChanged();
	}

	private void handleUpperSelectionChanged() {
		int index= fFilteredList.getSelectionIndex();

		fLowerList.removeAll();
		
		if (index < 0)
			return;
			
		fQualifierElements= fFilteredList.getFoldedElements(index);
		updateLowerListWidget(fQualifierElements);

		updateOkState();		
	}

	private void handleLowerSelectionChanged() {
		updateOkState();
	}
	
	// XXX name
	protected Object getWidgetSelection2() {
		int index= fLowerList.getSelectionIndex();
		
		if (index >= 0)
			return fQualifierElements[index];
		
		return null;
	}
		
	private void updateOkState() {
		Button okButton= getOkButton();
		if (okButton != null)
			okButton.setEnabled(getWidgetSelection() != null);
	}
	
	private void updateLowerListWidget(Object[] elements) {
		int length= elements.length;
		
		String[] qualifiers= new String[length];
		for (int i= 0; i != length; i++)
			qualifiers[i]= fQualifierRenderer.getText(elements[i]);

		TwoArrayQuickSorter sorter= new TwoArrayQuickSorter(isCaseIgnored());
		sorter.sort(qualifiers, elements);

		for (int i= 0; i != length; i++) {	
			TableItem item= new TableItem(fLowerList, SWT.NONE);
			item.setText(qualifiers[i]);
			item.setImage(fQualifierRenderer.getImage(elements[i]));
		}
		
		if (fLowerList.getItemCount() > 0)
			fLowerList.setSelection(0);
	}
	
}