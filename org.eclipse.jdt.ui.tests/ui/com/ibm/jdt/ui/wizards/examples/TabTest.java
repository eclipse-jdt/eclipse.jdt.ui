package com.ibm.jdt.ui.wizards.examples;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;

public class TabTest {
	private Shell fShell;
	private Label fLabel;
	
	public TabTest() {
	}
	
	public TabTest close () {
		if ((fShell != null) && (!fShell.isDisposed ())) fShell.dispose ();
		fShell= null;
		return this;
	}
	
	public TabTest open () {
		fShell= new Shell ();
		fShell.setText("TabTest");
		fShell.setLayout(new GridLayout());
		
		TabFolder folder= new TabFolder(fShell, SWT.NONE);	
		folder.setLayoutData(new GridData(GridData.FILL_BOTH));
		//folder.setLayout(new PageContainerFillLayout(0, 0, 20, 20));
		//folder.setLayout(new FillLayout());
	
		/*folder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				turnToPage(event);
			}
		});*/	
				
		TabItem item;
		Label label;
		
		item= new TabItem(folder, SWT.NONE);
		item.setText("Tab0");
		
		String[] addButtons= new String[] { "Add1", "Add2" };
		ListDialogField list= new ListDialogField(new Adapter(), addButtons, new LabelProvider(), 0);
		list.setRemoveButtonLabel("Remove");
		list.setLabelText("List: ");

		Composite c1= new Composite(folder, SWT.NONE);
		LayoutUtil.doDefaultLayout(c1, new DialogField[] { list }, true);
		
		item.setControl(c1);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText("Tab1");
		label= new Label(folder, SWT.LEFT);
		label.setText("Tab1");
		item.setControl(label);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText("Tab2");
		label= new Label(folder, SWT.LEFT);
		label.setText("Tab2");
		item.setControl(label);
		
		item= new TabItem(folder, SWT.NONE);
		item.setText("Tab3");
		label= new Label(folder, SWT.LEFT);
		label.setText("Tab3");
		item.setControl(label);
				
		fShell.setSize(400,500);
		fShell.open ();
		return this;
	}

	private class Adapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
		}		
				
		// -------- IListAdapter
		public void customButtonPressed(DialogField field, int index) {
		}
		
		public void selectionChanged(DialogField field) {
		}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
		}
	}
	
	public TabTest run () {
		Display display= fShell.getDisplay ();
		while (!fShell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		return this;
	}
	
	public static void main(java.lang.String[] args) {
		new TabTest().open().run().close();
	}
}
