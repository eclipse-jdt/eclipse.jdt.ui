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
package org.eclipse.jdt.internal.junit.wizards;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.CheckedTableSelectionDialog;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.Resources;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * An object contribution action that updates existing AllTests classes.
 */
public class UpdateTestSuite implements IObjectActionDelegate {
	private Shell fShell;
	private IPackageFragment fPack;
	private ICompilationUnit fTestSuite;
	private IMethod fSuiteMethod;
	private static boolean fEmptySelectionAllowed= false;
	private Object[] fSelectedTestCases;

	private class UpdateAllTestsValidator implements ISelectionStatusValidator {	
		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IType) {
					count++;
				}
			}
			if (count == 0 && !fEmptySelectionAllowed) {
				return new JUnitStatus(IStatus.ERROR, ""); //$NON-NLS-1$
			}
			
			IStatus recursiveInclusionStatus= checkRecursiveSuiteInclusion(selection);
			if (recursiveInclusionStatus != null && ! recursiveInclusionStatus.isOK())
				return recursiveInclusionStatus;
				
			String message;
			if (count == 1) {
				message= WizardMessages.getFormattedString("UpdateAllTests.selected_methods.label_one", new Integer(count)); //$NON-NLS-1$
			} else {
				message= WizardMessages.getFormattedString("UpdateAllTests.selected_methods.label_many", new Integer(count)); //$NON-NLS-1$
			}
			return new JUnitStatus(IStatus.INFO, message);
		}
		
		private IStatus checkRecursiveSuiteInclusion(Object[] selection){
			IType suiteClass= fSuiteMethod.getDeclaringType();
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IType){
					if (((IType)selection[i]).equals(suiteClass)){
						return new JUnitStatus(IStatus.WARNING, WizardMessages.getString("UpdateTestSuite.infinite_recursion")); //$NON-NLS-1$
					}
				}
			}
			return null;
		}
	}

	public UpdateTestSuite() {
		super();
	}

	/*
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/*
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {		
		ILabelProvider lprovider= new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
		IStructuredContentProvider cprovider= new NewTestSuiteCreationWizardPage.ClassesInSuitContentProvider();
	
		/* find TestClasses already in Test Suite */
		IType testSuiteType= fTestSuite.findPrimaryType();
		fSuiteMethod= testSuiteType.getMethod("suite", new String[] {}); //$NON-NLS-1$
		if (fSuiteMethod.exists()) {
			try {
			ISourceRange range= fSuiteMethod.getSourceRange();
			IBuffer buf= fTestSuite.getBuffer();
			String originalContent= buf.getText(range.getOffset(), range.getLength());
			buf.close();
			int start= originalContent.indexOf(NewTestSuiteCreationWizardPage.START_MARKER);
			if (start > -1) {
				if (originalContent.indexOf(NewTestSuiteCreationWizardPage.END_MARKER, start) > -1) {
					CheckedTableSelectionDialog dialog= new CheckedTableSelectionDialog(fShell, lprovider, cprovider);
					dialog.setValidator(new UpdateAllTestsValidator());
					dialog.setTitle(WizardMessages.getString("UpdateAllTests.title")); //$NON-NLS-1$
					dialog.setMessage(WizardMessages.getString("UpdateAllTests.message")); //$NON-NLS-1$
					dialog.setInitialSelections(cprovider.getElements(fPack));
					dialog.setSize(60, 25);
					dialog.setInput(fPack);
					if (dialog.open() == Window.OK) {
						fSelectedTestCases= dialog.getResult();
						try {
							PlatformUI.getWorkbench().getProgressService().busyCursorWhile(getRunnable());
						} catch (Exception e) {
							JUnitPlugin.log(e);
						}
					}
				} else {
					cannotUpdateSuiteError();
				}
			} else {
				cannotUpdateSuiteError();
			}
			} catch (JavaModelException e) {
				JUnitPlugin.log(e);
			}
		} else {
			noSuiteError();
		}
	}

	/*
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		fShell= JUnitPlugin.getActiveWorkbenchShell();		
		if (selection instanceof IStructuredSelection) {
			Object testSuiteObj= ((IStructuredSelection) selection).getFirstElement();
			if (testSuiteObj != null && testSuiteObj instanceof ICompilationUnit) {
				fTestSuite= (ICompilationUnit) testSuiteObj;
				IJavaElement packIJE= fTestSuite.getParent();
				if (packIJE instanceof IPackageFragment) {
					fPack= (IPackageFragment) packIJE;
				}
			}
		}
	}
	
	private void updateTestCasesInSuite(IProgressMonitor monitor) {

		try {
			monitor.beginTask(WizardMessages.getString("UpdateAllTests.beginTask"), 5); //$NON-NLS-1$
			if (! checkValidateEditStatus(fTestSuite, fShell))
				return;
				
			ISourceRange range= fSuiteMethod.getSourceRange();
			IBuffer buf= fTestSuite.getBuffer();
			String originalContent= buf.getText(range.getOffset(), range.getLength());
			StringBuffer source= new StringBuffer(originalContent);
			//using JDK 1.4
			//int start= source.toString().indexOf(NewTestSuiteCreationWizardPage.startMarker) --> int start= source.indexOf(NewTestSuiteCreationWizardPage.startMarker)
			int start= source.toString().indexOf(NewTestSuiteCreationWizardPage.START_MARKER);
			if (start > -1) {
				//using JDK 1.4
				//int end= source.toString().indexOf(NewTestSuiteCreationWizardPage.endMarker, start) --> int end= source.indexOf(NewTestSuiteCreationWizardPage.endMarker, start)
				int end= source.toString().indexOf(NewTestSuiteCreationWizardPage.END_MARKER, start);
				if (end > -1) {
					monitor.worked(1);
					end += NewTestSuiteCreationWizardPage.END_MARKER.length();
					//					String updatableCode= source.substring(start,end+NewTestSuiteCreationWizardPage.endMarker.length());
					source.replace(start, end, NewTestSuiteCreationWizardPage.getUpdatableString(fSelectedTestCases));
					buf.replace(range.getOffset(), range.getLength(), source.toString());
					monitor.worked(1);
					fTestSuite.reconcile();
					originalContent= buf.getText(0, buf.getLength());
					monitor.worked(1);
					String formattedContent=
						JUnitStubUtility.codeFormat(
							originalContent,
							0,
							JUnitStubUtility.getLineDelimiterUsed(fTestSuite));
					//buf.replace(range.getOffset(), range.getLength(), formattedContent);
					buf.replace(0, buf.getLength(), formattedContent);
					monitor.worked(1);
					fTestSuite.save(new SubProgressMonitor(monitor, 1), true);
				}
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, fShell, WizardMessages.getString("UpdateTestSuite.update"), WizardMessages.getString("UpdateTestSuite.error")); //$NON-NLS-1$ //$NON-NLS-2$
		} finally{
			monitor.done();
		}
	}
	
	static boolean checkValidateEditStatus(ICompilationUnit testSuiteCu, Shell shell){
		IStatus status= validateModifiesFiles(getTestSuiteFile(testSuiteCu));
		if (status.isOK())	
			return true;
		ErrorDialog.openError(shell, WizardMessages.getString("UpdateTestSuite.update"), WizardMessages.getString("UpdateTestSuite.could_not_update"), status); //$NON-NLS-1$ //$NON-NLS-2$
		return false;
	}
	
	private static IFile getTestSuiteFile(ICompilationUnit testSuiteCu){
		if (testSuiteCu.isWorkingCopy())
			return (IFile)testSuiteCu.getOriginalElement().getResource();
		else
			return (IFile)testSuiteCu.getResource();
	}
	
	private static IStatus validateModifiesFiles(IFile fileToModify) {
		IFile[] filesToModify= {fileToModify};
		IStatus status= Resources.checkInSync(filesToModify);
		if (! status.isOK())
			return status;
		status= Resources.makeCommittable(filesToModify, null);
		if (! status.isOK())
			return status;
		return new JUnitStatus();
	}

	public IRunnableWithProgress getRunnable() {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				if (monitor == null) {
					monitor= new NullProgressMonitor();
				}
				updateTestCasesInSuite(monitor);
			}
		};
	}

	private void cannotUpdateSuiteError() {
		MessageDialog.openError(fShell, WizardMessages.getString("UpdateAllTests.cannotUpdate.errorDialog.title"), //$NON-NLS-1$
			WizardMessages.getFormattedString("UpdateAllTests.cannotUpdate.errorDialog.message", new String[] {NewTestSuiteCreationWizardPage.START_MARKER, NewTestSuiteCreationWizardPage.END_MARKER})); //$NON-NLS-1$

	}

	private void noSuiteError() {
		MessageDialog.openError(fShell, WizardMessages.getString("UpdateAllTests.cannotFind.errorDialog.title"), WizardMessages.getString("UpdateAllTests.cannotFind.errorDialog.message")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
