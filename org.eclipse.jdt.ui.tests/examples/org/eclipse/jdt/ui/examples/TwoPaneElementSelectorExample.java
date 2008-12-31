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


import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.dialogs.TwoPaneElementSelector;




public class TwoPaneElementSelectorExample {


	public static void main(String[] args) {
		java.util.Random random= new java.util.Random();
		Object[] elements= new Object[8000];
		for (int i= 0; i < elements.length; i++)
			elements[i]= new Integer(random.nextInt()).toString();
		ILabelProvider elementRenderer= new org.eclipse.jface.viewers.LabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}
		};
		ILabelProvider qualfierRenderer= new org.eclipse.jface.viewers.LabelProvider() {
			public String getText(Object element) {
				return element.toString();
			}
		};
		Display display= new Display();
		TwoPaneElementSelector d= new TwoPaneElementSelector(new Shell(display), elementRenderer, qualfierRenderer);
		d.setTitle("Title");
		d.setMessage("this is a message");
		d.setElements(elements);

		d.open();

		Object res= d.getResult();
		System.out.println("res= "+res);
	}
}
