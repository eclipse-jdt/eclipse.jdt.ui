/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.examples;


import java.util.Random;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;


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
		
		int nLists= 3;
		
		Object[][] elements= new Object[nLists][];
		for (int i= 0; i < nLists; i++) {
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
		for (int i= 0; i < res.length; i++)
			System.out.println(res[i]);
	}
}