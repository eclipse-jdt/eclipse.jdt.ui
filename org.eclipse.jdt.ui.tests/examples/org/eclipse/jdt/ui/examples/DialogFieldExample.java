/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.examples;


import java.util.Random;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;


import org.eclipse.jface.viewers.LabelProvider;


import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.SelectionButtonDialogFieldGroup;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringButtonDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.StringDialogField;
import org.eclipse.swt.layout.GridData;


public class DialogFieldExample {
	private Shell fShell;
	private Label fLabel;
	
	public DialogFieldExample() {
	}
	
	public DialogFieldExample close () {
		if ((fShell != null) && (!fShell.isDisposed ())) fShell.dispose ();
		fShell= null;
		return this;
	}
	
	public DialogFieldExample open () {
		fShell= new Shell ();
		fShell.setText("Message Dialog Example");
		fShell.setLayout(new GridLayout());
		
		Adapter adapter= new Adapter();
		
		StringButtonDialogField string1= new StringButtonDialogField(adapter);
		string1.setLabelText("String1: ");
		string1.setText("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		
		StringButtonDialogField stringbutton= new StringButtonDialogField(adapter);
		stringbutton.setLabelText("StringButton: ");
		stringbutton.setButtonLabel("Click");
		stringbutton.setText("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

		
		String[] addButtons= new String[] { 
			/* 0 */ "Add1",
			/* 1 */ "Add2",
			/* 2 */ null,
			/* 3 */ "Up",
			/* 4 */ "Down",					
			/* 5 */ null,
			/* 6 */ "Remove"
		};
		ListDialogField list= new ListDialogField(adapter, addButtons, new LabelProvider());
		list.setUpButtonIndex(3);
		list.setDownButtonIndex(4);
		list.setRemoveButtonIndex(6);
		list.setLabelText("List: ");
		
		for (int i= 0; i < 30; i++) {
				list.addElement(i + "firstxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		}
		
		SelectionButtonDialogField selButton= new SelectionButtonDialogField(SWT.PUSH);
		selButton.setLabelText("Press Button");
		
		String[] radioButtons1= new String[] { "Option One", "Option Two", "Option Three" };
		SelectionButtonDialogFieldGroup rdgroup1= new SelectionButtonDialogFieldGroup(SWT.RADIO, radioButtons1, 3, 0);
		rdgroup1.setLabelText("Radio Button Group");
		
		String[] radioButtons2= new String[] { "Option One", "Option Two", "Option Three" };
		SelectionButtonDialogFieldGroup rdgroup2= new SelectionButtonDialogFieldGroup(SWT.CHECK, radioButtons2, 3);
		rdgroup2.setLabelText("Radio Button Group 2");		
		
		LayoutUtil.doDefaultLayout(fShell, new DialogField[] { string1, rdgroup2, stringbutton, selButton, list, rdgroup1 }, false);
		
		((GridData)string1.getTextControl(null).getLayoutData()).widthHint= 100;
		((GridData)stringbutton.getTextControl(null).getLayoutData()).widthHint= 100;
		
		fShell.setSize(fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		fShell.open ();
		return this;
	}
	
	private static Random fgRandom= new Random();
	
	private class Adapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter {
		
		// -------- IStringButtonAdapter
		public void changeControlPressed(DialogField field) {
		}		
				
		// -------- IListAdapter
		public void customButtonPressed(DialogField field, int index) {
			if (field instanceof ListDialogField) {
				ListDialogField list= (ListDialogField)field;
				list.addElement("elementxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-" + fgRandom.nextInt());
			}
		}
		
		public void selectionChanged(DialogField field) {}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
		}
	}	
	
	public DialogFieldExample run () {
		Display display= fShell.getDisplay ();
		while (!fShell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		return this;
	}
	
	public static void main(java.lang.String[] args) {
		new DialogFieldExample().open().run().close();
	}
}
