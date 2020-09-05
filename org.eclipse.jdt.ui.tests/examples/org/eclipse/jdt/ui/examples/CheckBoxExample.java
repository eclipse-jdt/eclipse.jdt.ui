/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
		CheckedListDialogField<String> list= new CheckedListDialogField<>(adapter, addButtons, new LabelProvider());
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

	private static class Adapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter<String> {

		// -------- IStringButtonAdapter
		@Override
		public void changeControlPressed(DialogField field) {
		}

		// -------- IListAdapter
		@Override
		public void customButtonPressed(ListDialogField<String> field, int index) {
			if (field instanceof CheckedListDialogField) {
				CheckedListDialogField<String> list= (CheckedListDialogField<String>)field;
				switch (index) {
				case 0:
					list.addElement("element-" + (fgRandom.nextInt() % 1000));
					break;

				case 2:
					System.out.println("---- printing all");
					List<String> checked= list.getCheckedElements();
					for (String checkedElement : checked) {
						System.out.println(checkedElement);
					}
					break;

				default:
					list.setChecked(list.getElement(0), true);
					break;
				}
			}
		}

		@Override
		public void selectionChanged(ListDialogField<String> field) {}

		// -------- IDialogFieldListener
		@Override
		public void dialogFieldChanged(DialogField field) {
		}
		@Override
		public void doubleClicked(ListDialogField<String> field) {
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
