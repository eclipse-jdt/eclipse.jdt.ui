/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.nls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.eclipse.ui.dialogs.SelectionDialog;

class ListDialog extends SelectionDialog {

	private IStructuredContentProvider fContentProvider;
	private ILabelProvider fLabelProvider;
	private Object fInput;
	private TableViewer fTableViewer;
	
	public ListDialog(Shell parent) {
		super(parent);
	}

	protected void setInput(Object input) {
		fInput= input;
	}
	
	protected void setContentProvider(IStructuredContentProvider sp){
		fContentProvider= sp;
	}
	
	protected void setLabelProvider(ILabelProvider lp){
		fLabelProvider= lp;
	}
	
	protected TableViewer getTableViewer(){
		return fTableViewer;
	}
		
	protected boolean hasFilters(){
		return fTableViewer.getFilters() != null && fTableViewer.getFilters().length != 0;
	}
	
	protected Control createDialogArea(Composite container) {
		Composite parent= (Composite) super.createDialogArea(container);
		createMessageArea(parent);
		fTableViewer= new TableViewer(parent);
		fTableViewer.setContentProvider(fContentProvider);
		final Table table= fTableViewer.getTable();
		final TableColumn c= new TableColumn(table, SWT.NULL);
		fTableViewer.getTable().addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				c.setWidth(table.getSize().x-2*table.getBorderWidth());
			}
		});

		fTableViewer.setLabelProvider(fLabelProvider);
		fTableViewer.setInput(fInput);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(15);
		gd.widthHint= convertWidthInCharsToPixels(50);
		table.setLayoutData(gd);
		return parent;
	}


}