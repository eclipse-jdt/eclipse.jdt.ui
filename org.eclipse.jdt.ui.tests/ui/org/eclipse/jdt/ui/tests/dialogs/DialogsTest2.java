/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.dialogs;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DialogCheck;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

public class DialogsTest2 extends TestCase {
	
	private static final String PROJECT_NAME = "DummyProject";

	public static Test suite() {
		TestSuite suite= new TestSuite(DialogsTest2.class.getName());
		suite.addTest(new DialogsTest2("testCheckedTreeSelectionDialog"));
		suite.addTest(new DialogsTest2("testCheckedTreeSelectionDialog"));
		return suite;
	}

	public DialogsTest2(String name) {
		super(name);
	}
	private Shell getShell() {
		return DialogCheck.getShell();
	}
	public void testCheckedTreeSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		Object initSelection= JavaProjectHelper.addSourceContainer(jproject, "src2");

		IJavaProject jproject2= JavaProjectHelper.createJavaProject("Project2", "bin");
		JavaProjectHelper.addSourceContainer(jproject2, "src1");
		JavaProjectHelper.addSourceContainer(jproject2, "src2");

		StandardJavaElementContentProvider provider= new StandardJavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT); 
		CheckedTreeSelectionDialog dialog= new CheckedTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setSorter(new JavaElementSorter());
		dialog.setTitle("Title"); //$NON-NLS-1$
		dialog.setMessage("Select the elements"); //$NON-NLS-1$
		
		dialog.setInput(jproject.getJavaModel());
		dialog.setInitialSelection(initSelection);
		dialog.setExpandedElements(new Object[] { initSelection, jproject });
		
		DialogCheck.assertDialog(dialog);
		
		System.out.println("Result:");
		Object[] checkedElements= dialog.getResult();
		for (int i= 0; i < checkedElements.length; i++) {
			System.out.println(labelProvider.getText(checkedElements[i]));
		}
		
		
		JavaProjectHelper.delete(jproject);	
		JavaProjectHelper.delete(jproject2);
	}

}

