/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Johannes Utzig <mail@jutzig.de> - [JUnit] Update test suite wizard for JUnit 4: @RunWith(Suite.class)... - https://bugs.eclipse.org/155828
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.eclipse.jdt.junit.wizards.NewTestSuiteWizardPage;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;
import org.eclipse.jdt.internal.junit.util.CheckedTableSelectionDialog;
import org.eclipse.jdt.internal.junit.util.ExceptionHandler;
import org.eclipse.jdt.internal.junit.util.JUnitStatus;
import org.eclipse.jdt.internal.junit.util.JUnitStubUtility;
import org.eclipse.jdt.internal.junit.util.Resources;

import org.eclipse.jdt.ui.JavaElementLabelProvider;

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
	private boolean fIsJunit4;
	private IAnnotation fSuiteClasses;

	private class UpdateAllTestsValidator implements ISelectionStatusValidator {
		/*
		 * @see ISelectionValidator#validate(Object[])
		 */
		public IStatus validate(Object[] selection) {
			int count= 0;
			for (Object element : selection) {
				if (element instanceof IType) {
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
			IType suiteClass= null;
			if (fIsJunit4) {
				suiteClass= (IType) fSuiteClasses.getParent();
			} else {
				suiteClass= fSuiteMethod.getDeclaringType();
			}
			for (Object element : selection) {
				if (element instanceof IType){
					if (((IType)element).equals(suiteClass)){
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
		SuiteClassesContentProvider cprovider= new SuiteClassesContentProvider(fIsJunit4);

		if (fIsJunit4) {
			/* find TestClasses already in Test Suite */
			IType testSuiteType= fTestSuite.findPrimaryType();
			fSuiteClasses= testSuiteType.getAnnotation("SuiteClasses"); //$NON-NLS-1$
			if (fSuiteClasses.exists()) {
				openTestSelectionDialog(lprovider, cprovider, testSuiteType);
			} else {
				noSuiteError();
			}
			
		} else{
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
						openTestSelectionDialog(lprovider, cprovider, testSuiteType);
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
	}

	private void openTestSelectionDialog(ILabelProvider lprovider, SuiteClassesContentProvider cprovider, IType testSuiteType) {
		CheckedTableSelectionDialog dialog= new CheckedTableSelectionDialog(fShell, lprovider, cprovider);
		dialog.setValidator(new UpdateAllTestsValidator());
		dialog.setTitle(WizardMessages.UpdateAllTests_title);
		dialog.setMessage(WizardMessages.UpdateAllTests_message);
		Set<IType> elements= cprovider.getTests(fPack);
		elements.remove(testSuiteType);
		dialog.setInitialSelections(elements.toArray());
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
				IType primaryType = fTestSuite.findPrimaryType();
				if (primaryType != null) {
					fIsJunit4 = primaryType.getAnnotation("RunWith").exists(); //$NON-NLS-1$
				}
				
			}
		}
	}

	public static void updateTestCasesInJunit4Suite(IProgressMonitor monitor, ICompilationUnit testSuite, IAnnotation testClassesAnnotation, Object[] selectedTestCases) throws JavaModelException {
		try {
			monitor.beginTask(WizardMessages.UpdateAllTests_beginTask, 5);

			ISourceRange range= testClassesAnnotation.getSourceRange();
			IDocument fullSource= new Document(testSuite.getBuffer().getContents());
			StringBuffer source= new StringBuffer();
			monitor.worked(1);
			source.append(getUpdatableAnnotations(selectedTestCases));
			fullSource.replace(range.getOffset(), range.getLength(), source.toString());
			monitor.worked(1);
			String formattedContent= JUnitStubUtility.formatCompilationUnit(testSuite.getJavaProject(), fullSource.get(), testSuite.findRecommendedLineSeparator());
			IBuffer buf= testSuite.getBuffer();
			buf.replace(0, buf.getLength(), formattedContent);
			monitor.worked(1);
			testSuite.save(new SubProgressMonitor(monitor, 1), true);
			monitor.worked(1);


		}  catch (BadLocationException e) {
			Assert.isTrue(false, "Should never happen"); //$NON-NLS-1$
		} finally{
			monitor.done();
		}
	}
	
	public static void updateTestCasesInSuite(IProgressMonitor monitor, ICompilationUnit testSuite, IMethod suiteMethod, Object[] selectedTestCases) throws JavaModelException {
		try {
			monitor.beginTask(WizardMessages.UpdateAllTests_beginTask, 5);

			ISourceRange range= suiteMethod.getSourceRange();
			IDocument fullSource= new Document(testSuite.getBuffer().getContents());
			String originalContent= fullSource.get(range.getOffset(), range.getLength());
			StringBuffer source= new StringBuffer(originalContent);
			TestSuiteClassListRange classRange= getTestSuiteClassListRange(source.toString());
			if (classRange != null) {
				monitor.worked(1);
				//					String updatableCode= source.substring(start,end+NewTestSuiteCreationWizardPage.endMarker.length());
				source.replace(classRange.getStart(), classRange.getEnd(), getUpdatableString(selectedTestCases));
				fullSource.replace(range.getOffset(), range.getLength(), source.toString());
				monitor.worked(1);
				String formattedContent= JUnitStubUtility.formatCompilationUnit(testSuite.getJavaProject(), fullSource.get(), testSuite.findRecommendedLineSeparator());
				//buf.replace(range.getOffset(), range.getLength(), formattedContent);
				IBuffer buf= testSuite.getBuffer();
				buf.replace(0, buf.getLength(), formattedContent);
				monitor.worked(1);
				testSuite.save(new SubProgressMonitor(monitor, 1), true);
				monitor.worked(1);
			}

		} catch (BadLocationException e) {
			Assert.isTrue(false, "Should never happen"); //$NON-NLS-1$
		} finally {
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
		for (Object selectedClasse : selectedClasses) {
			if (selectedClasse instanceof IType) {
				IType testType= (IType) selectedClasse;
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
	
	/*
	 * Returns the new test suite annotations which replace old annotations in the existing suite
	 */
	public static String getUpdatableAnnotations(Object[] selectedClasses) {
		StringBuffer buffer = new StringBuffer("@SuiteClasses({"); //$NON-NLS-1$
		for (int i= 0; i < selectedClasses.length; i++) {
			if (selectedClasses[i] instanceof IType) {
				IType testType= (IType) selectedClasses[i];
				buffer.append(testType.getElementName());
				buffer.append(".class"); //$NON-NLS-1$
				if (i < selectedClasses.length - 1)
					buffer.append(',');
			}
		}
		buffer.append("})"); //$NON-NLS-1$
		buffer.append("\n"); //$NON-NLS-1$
		return buffer.toString();
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
				if (! checkValidateEditStatus(fTestSuite, fShell))
					return;
				try {
					if (fIsJunit4)
						updateTestCasesInJunit4Suite(monitor, fTestSuite, fSuiteClasses, fSelectedTestCases);
					else
						updateTestCasesInSuite(monitor, fTestSuite, fSuiteMethod, fSelectedTestCases);
				} catch (JavaModelException e) {
					ExceptionHandler.handle(e, fShell, WizardMessages.UpdateTestSuite_update, WizardMessages.UpdateTestSuite_error);
				}
			}
		};
	}

	private void cannotUpdateSuiteError() {
		MessageDialog.openError(fShell, WizardMessages.UpdateAllTests_cannotUpdate_errorDialog_title,
			Messages.format(WizardMessages.UpdateAllTests_cannotUpdate_errorDialog_message, new String[] {NewTestSuiteWizardPage.START_MARKER, NewTestSuiteWizardPage.END_MARKER}));

	}

	private void noSuiteError() {
		if (fIsJunit4) {
			MessageDialog.openError(fShell, WizardMessages.UpdateAllTests_cannotFind_annotation_errorDialog_title, WizardMessages.UpdateAllTests_cannotFind_annotation_errorDialog_message);
		} else {
			MessageDialog.openError(fShell, WizardMessages.UpdateAllTests_cannotFind_errorDialog_title, WizardMessages.UpdateAllTests_cannotFind_errorDialog_message);
		}
	}
}
