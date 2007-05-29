/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

import org.eclipse.jdt.junit.wizards.NewTestSuiteWizardPage;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.CheckedTableSelectionDialog;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.Resources;

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
				message= Messages.format(WizardMessages.UpdateAllTests_selected_methods_label_one, new Integer(count)); 
			} else {
				message= Messages.format(WizardMessages.UpdateAllTests_selected_methods_label_many, new Integer(count)); 
			}
			return new JUnitStatus(IStatus.INFO, message);
		}
		
		private IStatus checkRecursiveSuiteInclusion(Object[] selection){
			IType suiteClass= fSuiteMethod.getDeclaringType();
			for (int i= 0; i < selection.length; i++) {
				if (selection[i] instanceof IType){
					if (((IType)selection[i]).equals(suiteClass)){
						return new JUnitStatus(IStatus.WARNING, WizardMessages.UpdateTestSuite_infinite_recursion); 
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
		IStructuredContentProvider cprovider= new SuiteClassesContentProvider();
	
		/* find TestClasses already in Test Suite */
		IType testSuiteType= fTestSuite.findPrimaryType();
		fSuiteMethod= testSuiteType.getMethod("suite", new String[] {}); //$NON-NLS-1$
		if (fSuiteMethod.exists()) {
			try {
			ISourceRange range= fSuiteMethod.getSourceRange();
			IBuffer buf= fTestSuite.getBuffer();
			String originalContent= buf.getText(range.getOffset(), range.getLength());
			buf.close();
			if (getTestSuiteClassListRange(originalContent) != null) {
				CheckedTableSelectionDialog dialog= new CheckedTableSelectionDialog(fShell, lprovider, cprovider);
				dialog.setValidator(new UpdateAllTestsValidator());
				dialog.setTitle(WizardMessages.UpdateAllTests_title); 
				dialog.setMessage(WizardMessages.UpdateAllTests_message); 
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
			monitor.beginTask(WizardMessages.UpdateAllTests_beginTask, 5); 
			if (! checkValidateEditStatus(fTestSuite, fShell))
				return;
				
			ISourceRange range= fSuiteMethod.getSourceRange();
			IDocument fullSource= new Document(fTestSuite.getBuffer().getContents());
			String originalContent= fullSource.get(range.getOffset(), range.getLength());
			StringBuffer source= new StringBuffer(originalContent);
			TestSuiteClassListRange classRange = getTestSuiteClassListRange(source.toString());
			if (classRange != null) {
				monitor.worked(1);
				//					String updatableCode= source.substring(start,end+NewTestSuiteCreationWizardPage.endMarker.length());
				source.replace(classRange.getStart(), classRange.getEnd(), getUpdatableString(fSelectedTestCases));
				fullSource.replace(range.getOffset(), range.getLength(), source.toString());
				monitor.worked(1);
				String formattedContent= JUnitStubUtility.formatCompilationUnit(fTestSuite.getJavaProject(), fullSource.get(), fTestSuite.findRecommendedLineSeparator());
				//buf.replace(range.getOffset(), range.getLength(), formattedContent);
				IBuffer buf= fTestSuite.getBuffer();
				buf.replace(0, buf.getLength(), formattedContent);
				monitor.worked(1);
				fTestSuite.save(new SubProgressMonitor(monitor, 1), true);
				monitor.worked(1);
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, fShell, WizardMessages.UpdateTestSuite_update, WizardMessages.UpdateTestSuite_error); 
		} catch (BadLocationException e) {
			Assert.isTrue(false, "Should never happen"); //$NON-NLS-1$
		} finally{
			monitor.done();
		}
	}
	
	public static TestSuiteClassListRange getTestSuiteClassListRange(String source) {
		int start= source.indexOf(NewTestSuiteWizardPage.NON_COMMENT_START_MARKER);
		if (start <= -1)
			return null;
		start = source.lastIndexOf(NewTestSuiteWizardPage.COMMENT_START, start);
		if (start <= -1)
			return null;
		int end= source.indexOf(NewTestSuiteWizardPage.NON_COMMENT_END_MARKER, start);
		if (end <= -1)
			return null;
		end += NewTestSuiteWizardPage.NON_COMMENT_END_MARKER.length();
		return new TestSuiteClassListRange(start, end);
	}

	/*
	 * Returns the new code to be included in a new suite() or which replaces old code in an existing suite().
	 */
	public static String getUpdatableString(Object[] selectedClasses) {
		StringBuffer suite= new StringBuffer();
		suite.append(NewTestSuiteWizardPage.START_MARKER+"\n"); //$NON-NLS-1$
		for (int i= 0; i < selectedClasses.length; i++) {
			if (selectedClasses[i] instanceof IType) {
				IType testType= (IType) selectedClasses[i];
				IMethod suiteMethod= testType.getMethod("suite", new String[] {}); //$NON-NLS-1$
				if (!suiteMethod.exists()) {
					suite.append("suite.addTestSuite("+testType.getElementName()+".class);"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					suite.append("suite.addTest("+testType.getElementName()+".suite());"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		suite.append("\n"+NewTestSuiteWizardPage.END_MARKER); //$NON-NLS-1$
		return suite.toString();
	}
	
	public static boolean checkValidateEditStatus(ICompilationUnit testSuiteCu, Shell shell){
		IStatus status= validateModifiesFiles(getTestSuiteFile(testSuiteCu));
		if (status.isOK())	
			return true;
		ErrorDialog.openError(shell, WizardMessages.UpdateTestSuite_update, WizardMessages.UpdateTestSuite_could_not_update, status); 
		return false;
	}
	
	private static IFile getTestSuiteFile(ICompilationUnit testSuiteCu) {
		return (IFile) testSuiteCu.getResource();
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
		MessageDialog.openError(fShell, WizardMessages.UpdateAllTests_cannotUpdate_errorDialog_title, 
			Messages.format(WizardMessages.UpdateAllTests_cannotUpdate_errorDialog_message, new String[] {NewTestSuiteWizardPage.START_MARKER, NewTestSuiteWizardPage.END_MARKER})); 

	}

	private void noSuiteError() {
		MessageDialog.openError(fShell, WizardMessages.UpdateAllTests_cannotFind_errorDialog_title, WizardMessages.UpdateAllTests_cannotFind_errorDialog_message); 
	}
}
