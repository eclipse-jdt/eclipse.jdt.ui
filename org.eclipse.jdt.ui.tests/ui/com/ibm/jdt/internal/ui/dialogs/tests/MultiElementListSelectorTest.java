package com.ibm.jdt.internal.ui.dialogs.tests;

import java.util.Random;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.jdt.internal.ui.dialogs.ISelectionValidator;
import org.eclipse.jdt.internal.ui.dialogs.MultiElementListSelectionDialog;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class MultiElementListSelectorTest {

	public static void main(String[] args) {

		ISelectionValidator validator= new ISelectionValidator() {
			public void isValid(Object[] selection, StatusInfo res) {
				if (selection != null && selection.length == 1) {
					res.setOK();
				} else {
					res.setError("Single selection");
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
		MultiElementListSelectionDialog d= new MultiElementListSelectionDialog(new Shell(display), "Title", null, elementRenderer, true, false);
		d.setMessage("this is a message");
		d.setValidator(validator);
		
		d.open(elements);
		
		Object[] res= d.getSelectedElements();
		for (int i= 0; i < res.length; i++)
			System.out.println(res[i]);
	}
}