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


public class TabExample {
	private Shell fShell;

	public TabExample() {
	}

	public TabExample close () {
		if ((fShell != null) && (!fShell.isDisposed ())) fShell.dispose ();
		fShell= null;
		return this;
	}

	public TabExample open () {
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

		String[] addButtons= new String[] {
			/* 0 */ "Add1",
			/* 1 */ "Add2",
			/* 2 */ null,
			/* 3 */ "Remove"
		};
		ListDialogField list= new ListDialogField(new Adapter(), addButtons, new LabelProvider());
		list.setRemoveButtonIndex(3);
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
		public void customButtonPressed(ListDialogField field, int index) {
		}

		public void selectionChanged(ListDialogField field) {
		}

		// -------- IDialogFieldListener
		public void dialogFieldChanged(DialogField field) {
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IListAdapter#doubleClicked(org.eclipse.jdt.internal.ui.wizards.dialogfields.ListDialogField)
		 */
		public void doubleClicked(ListDialogField field) {
		}
	}

	public TabExample run () {
		Display display= fShell.getDisplay ();
		while (!fShell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		return this;
	}

	public static void main(java.lang.String[] args) {
		new TabExample().open().run().close();
	}
}
