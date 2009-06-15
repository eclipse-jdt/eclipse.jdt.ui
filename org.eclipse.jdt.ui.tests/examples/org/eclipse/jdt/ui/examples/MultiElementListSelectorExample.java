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

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

/*
 * NOTE: Running this example will open the dialog but elements will appear
 * because the dialog internally uses a FilteredTree which only works when the
 * workbench is running.
 */
public class MultiElementListSelectorExample {


	public static void main(String[] args) {


		ISelectionStatusValidator validator= new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null && selection.length == 1) {
					return new StatusInfo();
				} else {
					StatusInfo status= new StatusInfo();
					status.setError("Single selection");
					return status;
				}

			}
		};



		Random random= new Random();

		ILabelProvider elementRenderer= new org.eclipse.jface.viewers.LabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}
		};

		int nPages= 3;
		Object[][] elements= new Object[nPages][];
		for (int i= 0; i < nPages; i++) {
			int size= random.nextInt(15);
			elements[i]= new String[size];
			for (int k= 0; k < size; k++) {
				elements[i][k]= "elem-" + i + "-" + k;
			}
		}
		
		Display display= new Display();
		MultiElementListSelectionDialog d= new MultiElementListSelectionDialog(new Shell(display), elementRenderer);
		d.setTitle("Title");
		d.setIgnoreCase(true);
		d.setMessage("this is a message");
		d.setValidator(validator);
		d.setElements(elements);

		d.open();

		Object[] res= d.getResult();
		if (res != null) {
			for (int i= 0; i < res.length; i++)
				System.out.println(res[i]);
		}
	}
}
