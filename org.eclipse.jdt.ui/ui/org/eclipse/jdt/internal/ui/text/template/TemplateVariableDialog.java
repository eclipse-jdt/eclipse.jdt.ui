/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.text.template;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;

import org.eclipse.jdt.internal.ui.dialogs.StatusDialog;

/**
 * A dialog to choose a template variable.
 */
public class TemplateVariableDialog extends StatusDialog implements ITableLabelProvider, IStructuredContentProvider {

	private TableViewer fTableViewer;
	private Button fOkButton;

	private final String[][] fVariables;
	
	private String fSelectedName;	

	/**
	 * Creates a template variable dialog.
	 */
	public TemplateVariableDialog(Shell shell, String[][] variables) {
		super(shell);
		fVariables= variables;
		
		setTitle(TemplateMessages.getString("TemplateVariableDialog.title")); //$NON-NLS-1$
	}
	
	/*
	 * @see Dialog#createDialogArea(Composite)
	 */
	protected Control createDialogArea(Composite ancestor) {
		Composite parent= new Composite(ancestor, SWT.NULL);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fTableViewer= new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		Table table= fTableViewer.getTable();
		
		GridData data= new GridData(GridData.FILL_BOTH);
		data.widthHint= convertWidthInCharsToPixels(80);
		data.heightHint= convertHeightInCharsToPixels(10);
		table.setLayoutData(data);
						
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		

		TableLayout tableLayout= new TableLayout();
		table.setLayout(tableLayout);
		
		TableColumn column1= new TableColumn(table, SWT.NULL);
		column1.setText(TemplateMessages.getString("TemplateVariableDialog.column.name")); //$NON-NLS-1$

		TableColumn column2= new TableColumn(table, SWT.NULL);
		column2.setText(TemplateMessages.getString("TemplateVariableDialog.column.description")); //$NON-NLS-1$
	
		tableLayout.addColumnData(new ColumnWeightData(30));
		tableLayout.addColumnData(new ColumnWeightData(70));
		
		fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent e) {
				buttonPressed(IDialogConstants.OK_ID);
			}
		});
		
		fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				IStructuredSelection selection= (IStructuredSelection) fTableViewer.getSelection();
				Object element= selection.getFirstElement();
				
				fSelectedName= (element == null)
					? null
					: ((String[]) element)[0];
				
				updateButtons();
			}
		});
		
		fTableViewer.setLabelProvider(this);
		fTableViewer.setContentProvider(this);
		fTableViewer.setInput(fVariables);
		
		return parent;
	}

	/*
	 * @see Dialog.createButtonsForButtonBar(Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		fOkButton= createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);

		fOkButton.setText(TemplateMessages.getString("TemplateVariableDialog.insert")); //$NON-NLS-1$

		updateButtons();
	}

	private void updateButtons() {
		int selectionCount= ((IStructuredSelection) fTableViewer.getSelection()).size();
		int itemCount= fTableViewer.getTable().getItemCount();
		
		fOkButton.setEnabled(selectionCount == 1);
	}

	/**
	 * Returns the selected variable name.
	 */	
	public String getSelectedName() {
		return fSelectedName;
	}
	
	/*
	 * @see IStructuredContentProvider#getElements(Object)
	 */
	public Object[] getElements(Object inputElement) {
		return (Object[]) inputElement;
	}

	/*
	 * @see IBaseLabelProvider#addListener(ILabelProviderListener)
	 */
	public void addListener(ILabelProviderListener listener) {
	}

	/*
	 * @see IBaseLabelProvider#dispose()
	 */
	public void dispose() {
	}

	/*
	 * @see IBaseLabelProvider#isLabelProperty(Object, String)
	 */
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	/*
	 * @see IBaseLabelProvider#removeListener(ILabelProviderListener)
	 */
	public void removeListener(ILabelProviderListener listener) {
	}

	/*
	 * @see IContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

	/*
	 * @see ITableLabelProvider#getColumnImage(Object, int)
	 */
	public Image getColumnImage(Object element, int columnIndex) {
		return null;
	}

	/*
	 * @see ITableLabelProvider#getColumnText(Object, int)
	 */
	public String getColumnText(Object element, int columnIndex) {
		return ((String[]) element)[columnIndex];
	}

}

