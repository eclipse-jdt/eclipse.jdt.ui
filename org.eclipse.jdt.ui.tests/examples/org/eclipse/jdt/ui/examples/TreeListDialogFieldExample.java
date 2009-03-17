/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

		TreeListDialogField list= new TreeListDialogField(adapter, addButtons, new LabelProvider());
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

	private class Adapter implements ITreeListAdapter {

		// -------- ITreeListAdapter
		public void customButtonPressed(TreeListDialogField field, int index) {
			field.addElement("elementxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx-" + fgRandom.nextInt());
		}

		public void selectionChanged(TreeListDialogField field) {}

		public Object[] getChildren(TreeListDialogField field, Object element) {
			if (field.getElements().contains(element)) {
				return new String[] {
					"Source Attachment: c:/hello/z.zip",
					"Javadoc Location: http://www.oo.com/doc"
				};
			}
			return new String[0];
		}

		public Object getParent(TreeListDialogField field, Object element) {
			return null;
		}

		public boolean hasChildren(TreeListDialogField field, Object element) {
			return field.getElements().contains(element);
		}

		public void doubleClicked(TreeListDialogField field) {
		}

		public void keyPressed(TreeListDialogField field, KeyEvent event) {
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
