/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.jdt.internal.ui.reorg.*;

class ListDialog extends Dialog {
	private IStructuredContentProvider fContentProvider;
	private ILabelProvider fLabelProvider;
	private Object fInput;
	private String fTitle;
		
	public ListDialog(Shell parent, Object input, String title, String message, IStructuredContentProvider sp, ILabelProvider lp) {
		super(parent);
		fTitle= title;
		setShellStyle(getShellStyle() | SWT.RESIZE);
		fInput= input;
		fContentProvider= sp;
		fLabelProvider= lp;
	}

	/* (non-Javadoc)
	 * Method declared in Window.
	 */
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		if (fTitle != null)
			shell.setText(fTitle);
	}
	
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	}
	protected Control createDialogArea(Composite container) {
		Composite parent= (Composite) super.createDialogArea(container);
		TableViewer v= new TableViewer(parent);
		v.setContentProvider(fContentProvider);
		final Table table= v.getTable();
		final TableColumn c= new TableColumn(table, SWT.NULL);
		v.getTable().addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				c.setWidth(table.getSize().x-2*table.getBorderWidth());
			}
		});

		v.setLabelProvider(fLabelProvider);
		v.setInput(fInput);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.heightHint= convertHeightInCharsToPixels(15);
		gd.widthHint= convertWidthInCharsToPixels(100);
		table.setLayoutData(gd);
		return table;
	}
}