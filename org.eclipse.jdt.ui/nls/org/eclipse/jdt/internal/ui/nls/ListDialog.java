/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.nls;
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
import org.eclipse.ui.dialogs.SelectionDialog;
class ListDialog extends SelectionDialog {
	private IStructuredContentProvider fContentProvider;
	private ILabelProvider fLabelProvider;
	private Object fInput;
	
	public ListDialog(Shell parent, Object input, String title, String message, IStructuredContentProvider sp, ILabelProvider lp) {
		super(parent);
		setTitle(title);
		setMessage(message);
		fInput= input;
		fContentProvider= sp;
		fLabelProvider= lp;
	}

	protected Control createDialogArea(Composite container) {
		Composite parent= (Composite) super.createDialogArea(container);
		createMessageArea(parent);
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
		gd.widthHint= convertWidthInCharsToPixels(50);
		table.setLayoutData(gd);
		return table;
	}
}