/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.ui;

 
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchResultCollector;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * A dialog to select a test method.
 */
public class TestMethodSelectionDialog extends ElementListSelectionDialog {

	private IJavaElement fElement;

	public static class TestReferenceCollector implements IJavaSearchResultCollector {
		IProgressMonitor fMonitor;
		Set fResult= new HashSet(200);
		
		public TestReferenceCollector(IProgressMonitor pm) {
			fMonitor= pm;
		}
		
		public void aboutToStart() {
		}
	
		public void accept(IResource resource, int start, int end, IJavaElement enclosingElement, int accuracy) {
			if (enclosingElement.getElementName().startsWith("test")) //$NON-NLS-1$
				fResult.add(enclosingElement);
		}
	
		public void done() {
		}
	
		public IProgressMonitor getProgressMonitor() {
			return fMonitor;
		}
		
		public Object[] getResult() {
			return fResult.toArray();
		}
	}

	public TestMethodSelectionDialog(Shell shell, IJavaElement element) {
		super(shell, new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_POST_QUALIFIED));
		fElement= element;
	}
	
	/*
	 * @see Windows#configureShell
	 */
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		WorkbenchHelp.setHelp(newShell, IJUnitHelpContextIds.TEST_SELECTION_DIALOG);
	}

	/*
	 * @see Window#open()
	 */
	public int open() {
		Object[] elements;
		IType testType= findTestType();
		
		if (testType == null) 
			return CANCEL;
		
		try {
			elements= searchTestMethods(fElement, testType);
		} catch (InterruptedException e) {
			return CANCEL;
		} catch (InvocationTargetException e) {
			MessageDialog.openError(getParentShell(), JUnitMessages.getString("TestMethodSelectionDialog.error.title"), e.getTargetException().getMessage()); //$NON-NLS-1$
			return CANCEL;
		}
		
		if (elements.length == 0) {
			String msg= JUnitMessages.getFormattedString("TestMethodSelectionDialog.notfound_message", fElement.getElementName()); //$NON-NLS-1$
			MessageDialog.openInformation(getParentShell(), JUnitMessages.getString("TestMethodSelectionDialog.no_tests.title"), msg); //$NON-NLS-1$
			return CANCEL;
		}
		setElements(elements);
		return super.open();
	}
	
	private IType findTestType() {
		String qualifiedName= JUnitPlugin.TEST_INTERFACE_NAME;
		IJavaProject[] projects;
		Set result= new HashSet();
		try {
			projects= JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()).getJavaProjects();
			for (int i= 0; i < projects.length; i++) {
				IJavaProject project= projects[i];
				IType type= project.findType(qualifiedName);
				if (type != null) 
					result.add(type);
			}
		} catch (JavaModelException e) {
			ErrorDialog.openError(getParentShell(), JUnitMessages.getString("TestMethodSelectionDialog.error.notfound.title"), JUnitMessages.getString("TestMethodSelectionDialog.error.notfound.message"), e.getStatus()); //$NON-NLS-1$ //$NON-NLS-2$
			return null;
		}
		if (result.size() == 0) {
			String msg= JUnitMessages.getFormattedString("TestMethodSelectionDialog.test_not_found", JUnitPlugin.TEST_INTERFACE_NAME); //$NON-NLS-1$
			MessageDialog.openError(getParentShell(), JUnitMessages.getString("TestMethodSelectionDialog.select_dialog.title"), msg); //$NON-NLS-1$
			return null;
		}
		if (result.size() == 1)
			return (IType)result.toArray()[0];
		
		return selectTestType(result);
	}
	
	private IType selectTestType(Set result) {
		ILabelProvider labelProvider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_PARAMETERS | JavaElementLabelProvider.SHOW_ROOT);
		ElementListSelectionDialog dialog= new ElementListSelectionDialog(getParentShell(), labelProvider);
		dialog.setTitle(JUnitMessages.getString("TestMethodSelectionDialog.dialog.title"));  //$NON-NLS-1$
		String msg= JUnitMessages.getFormattedString("TestMethodSelectionDialog.testproject", "junit.framework.Test"); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.setMessage(msg);
		IJavaProject[] projects= new IJavaProject[result.size()];
		IType[] testTypes= (IType[]) result.toArray(new IType[result.size()]);
		for (int i= 0; i < projects.length; i++) 
			projects[i]= testTypes[i].getJavaProject();
		dialog.setElements(projects);
		if (dialog.open() == Window.CANCEL)	
			return null;
		IJavaProject project= (IJavaProject) dialog.getFirstResult();
		for (int i= 0; i < testTypes.length; i++) {
			if (testTypes[i].getJavaProject().equals(project))
				return testTypes[i];
		}
		return null;	
	}
	
	public Object[] searchTestMethods(final IJavaElement element, final IType testType) throws InvocationTargetException, InterruptedException  {
		final TestReferenceCollector[] col= new TestReferenceCollector[1];
		
		IRunnableWithProgress runnable= new IRunnableWithProgress() {
			public void run(IProgressMonitor pm) throws InvocationTargetException {
				try {
					col[0]= doSearchTestMethods(element, testType, pm);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		return col[0].getResult();
	}

	private TestReferenceCollector doSearchTestMethods(IJavaElement element, IType testType, IProgressMonitor pm) throws JavaModelException{
		IJavaSearchScope scope= SearchEngine.createHierarchyScope(testType);
		TestReferenceCollector collector= new TestReferenceCollector(pm);
		new SearchEngine().search(ResourcesPlugin.getWorkspace(), element, IJavaSearchConstants.REFERENCES, scope, collector);
		return collector;
	}
}
