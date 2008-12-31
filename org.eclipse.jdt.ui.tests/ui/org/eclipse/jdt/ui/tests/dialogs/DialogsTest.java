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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.util.DialogCheck;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

public class DialogsTest extends TestCase {

	private static final String PROJECT_NAME = "DummyProject";

	public static Test suite() {
		TestSuite suite= new TestSuite(DialogsTest.class.getName());
//		suite.addTest(new DialogsTest("testElementListSelectionDialog2"));
		suite.addTest(new DialogsTest("testElementListSelectionDialog"));
		suite.addTest(new DialogsTest("testMultiElementSelectionDialog"));
		suite.addTest(new DialogsTest("testTwoPaneSelectionDialog"));
		suite.addTest(new DialogsTest("testElementTreeSelectionDialog"));
		suite.addTest(new DialogsTest("testElementListSelectionDialog"));
		return suite;
	}

	public DialogsTest(String name) {
		super(name);
	}
	private Shell getShell() {
		return DialogCheck.getShell();
	}
	/*
	public void testTwoPaneSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		JavaProjectHelper.addRTJar(jproject);

		OpenTypeSelectionDialog dialog= new OpenTypeSelectionDialog(getShell(), new ProgressMonitorDialog(getShell()),
			IJavaSearchConstants.TYPE, SearchEngine.createWorkspaceScope());

		dialog.setTitle("Open Type");
		dialog.setMessage("&Choose a type (? = any character, * = any string):");

		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}
	*/
	/*
	private TypeInfo[] getRefs(ArrayList list, int off, int len) {
		TypeInfo[] res= new TypeInfo[len];
		for (int i= 0; i < len; i++) {
			res[i]= (TypeInfo) list.get(off + i);
		}
		return res;
	}


	public void testMultiElementSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		JavaProjectHelper.addRTJar(jproject);

		ILabelProvider labelProvider= new TypeInfoLabelProvider(TypeInfoLabelProvider.SHOW_FULLYQUALIFIED);

		ArrayList list= new ArrayList(200);

		IJavaSearchScope searchScope= SearchEngine.createJavaSearchScope(new IJavaElement[] { jproject });
		AllTypesCache.getTypes(searchScope, IJavaSearchConstants.TYPE, null, list);

		MultiElementListSelectionDialog dialog= new MultiElementListSelectionDialog(getShell(), labelProvider);
		dialog.setTitle("Title"); //$NON-NLS-1$
		dialog.setMessage("Description:"); //$NON-NLS-1$

		assertTrue(list.size() > 15);
		TypeInfo[][] refs= new TypeInfo[][] { getRefs(list, 0, 3), getRefs(list, 4, 6), getRefs(list, 10, 5) };
		dialog.setElements(refs);
		dialog.setInitialSelections(new Object[refs.length]);

		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);

	}
	*/

	public void testElementTreeSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		Object initSelection= JavaProjectHelper.addSourceContainer(jproject, "src2");

		IJavaProject jproject2= JavaProjectHelper.createJavaProject("Project2", "bin");
		JavaProjectHelper.addSourceContainer(jproject2, "src1");
		JavaProjectHelper.addSourceContainer(jproject2, "src2");

		StandardJavaElementContentProvider provider= new StandardJavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setComparator(new JavaElementComparator());
		dialog.setTitle("Title");
		dialog.setMessage("Message");

		dialog.setInput(jproject.getJavaModel());
		dialog.setInitialSelection(initSelection);

		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
		JavaProjectHelper.delete(jproject2);
	}

	public void testElementListSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		IPackageFragmentRoot root=  JavaProjectHelper.addRTJar(jproject);
		assertTrue(root != null);
		Object[] elements= root.getChildren();

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
		dialog.setIgnoreCase(false);
		dialog.setTitle("Title");
		dialog.setMessage("Message");
		dialog.setEmptyListMessage("empty list");
		dialog.setElements(elements);

		DialogCheck.assertDialog(dialog);

		JavaProjectHelper.delete(jproject);
	}


	private static class TestLabelProvider extends LabelProvider {
		public Image getImage(Object element) {
			return null;
		}

		public String getText(Object element) {
			Integer i= (Integer) element;
			return "e-" + i.toString();
		}
	}

	public void testElementListSelectionDialog2() throws Exception {
		Object[] elements= new Integer[] {
			new Integer(0),
			new Integer(1),
			new Integer(2),
			new Integer(7),
			new Integer(12),
			new Integer(42)
		};

		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getShell(), new TestLabelProvider());
		dialog.setIgnoreCase(false);
		dialog.setTitle("Title");
		dialog.setMessage("Message");
		dialog.setEmptyListMessage("empty messgae");
		dialog.setElements(elements);
		dialog.setInitialSelections(new Object[] {new Integer(7)});

		DialogCheck.assertDialog(dialog);

		Object[] results= dialog.getResult();
		assertTrue(results.length == 1);
		assertEquals(new Integer(7), results[0]);
	}

}

