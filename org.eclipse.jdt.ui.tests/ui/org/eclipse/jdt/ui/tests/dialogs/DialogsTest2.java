/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.dialogs;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.viewers.ILabelProvider;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.internal.WorkbenchPlugin;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.TestPluginLauncher;
import org.eclipse.jdt.testplugin.util.DialogCheck;
import org.eclipse.jdt.ui.JavaElementContentProvider;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaElementSorter;

import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;

public class DialogsTest2 extends TestCase {
	
	private static final String PROJECT_NAME = "DummyProject";

	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), DialogsTest2.class, args);
	}

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
	private IWorkbench getWorkbench() {
		return WorkbenchPlugin.getDefault().getWorkbench();
	}
	
	
	public void testCheckedTreeSelectionDialog() throws Exception {
		IJavaProject jproject= JavaProjectHelper.createJavaProject(PROJECT_NAME, "bin");
		JavaProjectHelper.addSourceContainer(jproject, "src1");
		Object initSelection= JavaProjectHelper.addSourceContainer(jproject, "src2");

		IJavaProject jproject2= JavaProjectHelper.createJavaProject("Project2", "bin");
		JavaProjectHelper.addSourceContainer(jproject2, "src1");
		JavaProjectHelper.addSourceContainer(jproject2, "src2");

		JavaElementContentProvider provider= new JavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT); 
		CheckedTreeSelectionDialog dialog= new CheckedTreeSelectionDialog(getShell(), labelProvider, provider);
		dialog.setSorter(new JavaElementSorter());
		dialog.setTitle(NewWizardMessages.getString("ContainerPage.ChooseSourceContainerDialog.title")); //$NON-NLS-1$
		dialog.setMessage(NewWizardMessages.getString("ContainerPage.ChooseSourceContainerDialog.description")); //$NON-NLS-1$
		
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

