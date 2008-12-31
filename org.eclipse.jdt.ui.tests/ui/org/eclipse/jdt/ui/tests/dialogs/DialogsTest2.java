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
package org.eclipse.jdt.ui.tests.dialogs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DialogCheck;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.dialogs.SelectionDialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

public class DialogsTest2 extends TestCase {

	private static final String PROJECT_NAME = "DummyProject";

	public static Test suite() {
		TestSuite suite= new TestSuite(DialogsTest2.class.getName());
		suite.addTest(new DialogsTest2("testCheckedTreeSelectionDialog"));
		suite.addTest(new DialogsTest2("testCheckedTreeSelectionDialog"));
		suite.addTest(new DialogsTest2("testPackageSelectionDialog01"));
		suite.addTest(new DialogsTest2("testPackageSelectionDialog02"));
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
		dialog.setComparator(new JavaElementComparator());
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

	public void testPackageSelectionDialog01() throws CoreException, IOException, InvocationTargetException {
		IJavaProject jproject= JavaProjectHelper.createJavaProjectWithJUnitSource(PROJECT_NAME, "src", "bin");

		SelectionDialog dialog= JavaUI.createPackageDialog(getShell(), jproject, IJavaElementSearchConstants.CONSIDER_BINARIES, "");
		dialog.setBlockOnOpen(false);
		dialog.open();

		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

	public void testPackageSelectionDialog02() throws CoreException, IOException, InvocationTargetException {
		IJavaProject jproject= JavaProjectHelper.createJavaProjectWithJUnitSource(PROJECT_NAME, "src", "bin");

		IPackageFragmentRoot root= jproject.getPackageFragmentRoots()[0];
		SelectionDialog dialog= JavaUI.createPackageDialog(getShell(), root, "");
		dialog.setBlockOnOpen(false);
		dialog.open();

		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}

}
