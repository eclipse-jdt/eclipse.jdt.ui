/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.examples;


import java.util.List;
import java.util.Random;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.CheckedListDialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField;


public class CheckBoxExample {
	private Shell fShell;

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



		String[] addButtons= new String[] {
			/* 0 */ "Add1",
			/* 1 */ "Check 0",
			/* 2 */ "Print",
			/* 3 */ null,
			/* 4 */ "Check All",
			/* 5 */ "Uncheck All",
			/* 6 */ null,
			/* 7 */ "Remove"
		};
		CheckedListDialogField list= new CheckedListDialogField(adapter, addButtons, new LabelProvider());
		list.setCheckAllButtonIndex(4);
		list.setUncheckAllButtonIndex(5);
		list.setRemoveButtonIndex(7);
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
		public void customButtonPressed(ListDialogField field, int index) {
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

		public void selectionChanged(ListDialogField field) {}

		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void doubleClicked(ListDialogField field) {
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
