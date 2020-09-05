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
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.ITreeListAdapter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.TreeListDialogField;


public class TreeListDialogFieldExample {
	private Shell fShell;

	public TreeListDialogFieldExample() {
	}

	public TreeListDialogFieldExample close () {
		if ((fShell != null) && (!fShell.isDisposed ())) fShell.dispose ();
		fShell= null;
		return this;
	}



	public TreeListDialogFieldExample open () {
		fShell= new Shell ();
		fShell.setText("Message Dialog Example");
		fShell.setLayout(new GridLayout());

		Adapter adapter= new Adapter();


		String[] addButtons= new String[] {
			/* 0 */ "Add1",
			/* 1 */ "Add2",
			/* 2 */ null,
			/* 3 */ "Up",
			/* 4 */ "Down",
			/* 5 */ null,
			/* 6 */ "Remove"
		};

		TreeListDialogField<String> list= new TreeListDialogField<>(adapter, addButtons, new LabelProvider());
		list.setUpButtonIndex(3);
		list.setDownButtonIndex(4);
		list.setRemoveButtonIndex(6);
		list.setLabelText("List: ");


		for (int i= 0; i < 30; i++) {
				list.addElement(i + "firstxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
		}


		LayoutUtil.doDefaultLayout(fShell, new DialogField[] { list }, false);

		fShell.setSize(fShell.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		fShell.open ();
		return this;
	}

	private static Random fgRandom= new Random();

	private static class Adapter implements ITreeListAdapter<String> {

		// -------- ITreeListAdapter
		@Override
		public void customButtonPressed(TreeListDialogField<String> field, int index) {
			field.addElement("elementxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-" + fgRandom.nextInt());
		}

		@Override
		public void selectionChanged(TreeListDialogField<String> field) {}

		@Override
		public Object[] getChildren(TreeListDialogField<String> field, Object element) {
			if (field.getElements().contains(element)) {
				return new String[] {
					"Source Attachment: c:/hello/z.zip",
					"Javadoc Location: http://www.oo.com/doc"
				};
			}
			return new String[0];
		}

		@Override
		public Object getParent(TreeListDialogField<String> field, Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(TreeListDialogField<String> field, Object element) {
			return field.getElements().contains(element);
		}

		@Override
		public void doubleClicked(TreeListDialogField<String> field) {
		}

		@Override
		public void keyPressed(TreeListDialogField<String> field, KeyEvent event) {
		}

	}

	public TreeListDialogFieldExample run () {
		Display display= fShell.getDisplay ();
		while (!fShell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		return this;
	}

	public static void main(java.lang.String[] args) {
		new TreeListDialogFieldExample().open().run().close();
	}
}
