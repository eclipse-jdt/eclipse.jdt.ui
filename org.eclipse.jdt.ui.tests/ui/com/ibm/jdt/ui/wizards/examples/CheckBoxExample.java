package com.ibm.jdt.ui.wizards.examples;

import java.util.List;
import java.util.Random;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;

public class CheckBoxExample {
	private Shell fShell;
	private Label fLabel;
	
	public CheckBoxExample() {
	}
	
	public CheckBoxExample close () {
		if ((fShell != null) && (!fShell.isDisposed ())) fShell.dispose ();
		fShell= null;
		return this;
	}
	
	public CheckBoxExample open () {
		fShell= new Shell ();
		fShell.setText("Message Dialog Example");
		fShell.setLayout(new GridLayout());
		
		Adapter adapter= new Adapter();

		
		String[] addButtons= new String[] { "Add1", "Check 0", "Print"};
		CheckedListDialogField list= new CheckedListDialogField(adapter, addButtons, new LabelProvider(), 0);
		list.setRemoveButtonLabel("Remove");
		list.setLabelText("List: ");
				
		LayoutUtil.doDefaultLayout(fShell, new DialogField[] { list }, false);
		
		fShell.setSize(400,500);
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
			if (field instanceof CheckedListDialogField) {
				CheckedListDialogField list= (CheckedListDialogField)field;
				if (index == 0) {
					list.addElement("element-" + (fgRandom.nextInt() % 1000));
				} else if (index == 2) {
					System.out.println("---- printing all");
					List checked= list.getCheckedElements();
					for (int i= 0; i < checked.size(); i++) {
						System.out.println(checked.get(i).toString());
					}
				} else {
					list.setChecked(list.getElement(0), true);
				}	
			}
		}
		
		public void selectionChanged(DialogField field) {}
		
		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
		}
	}	
	
	public CheckBoxExample run () {
		Display display= fShell.getDisplay ();
		while (!fShell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		return this;
	}
	
	public static void main(java.lang.String[] args) {
		new CheckBoxExample().open().run().close();
	}
}
