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


import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ITableLabelProvider;
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


public class DialogFieldExample {
	private Shell fShell;

	public DialogFieldExample() {
	}

	public DialogFieldExample close () {
		if ((fShell != null) && (!fShell.isDisposed ())) fShell.dispose ();
		fShell= null;
		return this;
	}

	private static class MylabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		@Override
		public String getColumnText(Object element, int columnIndex) {
			if (columnIndex == 0) {
				return element.toString();
			} else {
				return "" + columnIndex;
			}
		}
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
		String[] columnHeaders= new String[] { "Name", "Number" };


		ListDialogField<String> list= new ListDialogField<>(adapter, addButtons, new MylabelProvider());
		list.setUpButtonIndex(3);
		list.setDownButtonIndex(4);
		list.setRemoveButtonIndex(6);
		list.setLabelText("List: ");

		list.setTableColumns(new ListDialogField.ColumnsDescription(columnHeaders, true));

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

	private static class Adapter implements IStringButtonAdapter, IDialogFieldListener, IListAdapter<String> {

		// -------- IStringButtonAdapter
		@Override
		public void changeControlPressed(DialogField field) {
		}

		// -------- IListAdapter
		@Override
		public void customButtonPressed(ListDialogField<String> field, int index) {
			if (field != null) {
				field.addElement("elementxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-" + fgRandom.nextInt());
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
