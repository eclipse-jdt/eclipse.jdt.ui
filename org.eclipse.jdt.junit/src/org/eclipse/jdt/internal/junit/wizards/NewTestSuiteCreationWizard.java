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
package org.eclipse.jdt.internal.junit.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IResource;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.IEditorPart;

import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;

import org.eclipse.jdt.junit.wizards.NewTestSuiteWizardPage;

import org.eclipse.jdt.internal.junit.BasicElementLabels;
import org.eclipse.jdt.internal.junit.Messages;
import org.eclipse.jdt.internal.junit.ui.JUnitPlugin;

/**
 * A wizard for creating test suites.
 */
public class NewTestSuiteCreationWizard extends JUnitWizard {

	private NewTestSuiteWizardPage fPage;
	
	public NewTestSuiteCreationWizard() {
		super();
		setWindowTitle(WizardMessages.Wizard_title_new_testsuite); 
		initDialogSettings();
	}

	/*
	 * @see Wizard#createPages
	 */	
	public void addPages() {
		super.addPages();
		fPage= new NewTestSuiteWizardPage();
		addPage(fPage);
		fPage.init(getSelection());
	}	

	/*
	 * @see Wizard#performFinish
	 */		
	public boolean performFinish() {
		IPackageFragment pack= fPage.getPackageFragment();
		String filename= fPage.getTypeName() + ".java"; //$NON-NLS-1$
		ICompilationUnit cu= pack.getCompilationUnit(filename);
		if (cu.exists()) {
			IEditorPart cu_ep= EditorUtility.isOpenInEditor(cu);
			if (cu_ep != null && cu_ep.isDirty()) {
				boolean saveUnsavedChanges= 
					MessageDialog.openQuestion(fPage.getShell(), 
						WizardMessages.NewTestSuiteWiz_unsavedchangesDialog_title, 
						Messages.format(WizardMessages.NewTestSuiteWiz_unsavedchangesDialog_message, 
						BasicElementLabels.getResourceName(filename)));
				if (saveUnsavedChanges) {
					try {
						getContainer().run(false, false, getRunnableSave(cu_ep));
					} catch (Exception e) {
						JUnitPlugin.log(e);
					}
				}
			}
			IType suiteType= cu.getType(fPage.getTypeName());
			IMethod suiteMethod= suiteType.getMethod("suite", new String[] {}); //$NON-NLS-1$
			if (suiteMethod.exists()) {
				try {
				ISourceRange range= suiteMethod.getSourceRange();
				IBuffer buf= cu.getBuffer();
				String originalContent= buf.getText(range.getOffset(), range.getLength());
				if (UpdateTestSuite.getTestSuiteClassListRange(originalContent) == null) {
					cannotUpdateSuiteError();
					return false;
				}
				} catch (JavaModelException e) {
					JUnitPlugin.log(e);
					return false;
				}
			}
		}
		
		if (finishPage(fPage.getRunnable())) {
			if (!fPage.hasUpdatedExistingClass())
				postCreatingType();	
			return true;
		}

		return false;		
	}
	
	private void cannotUpdateSuiteError() {
		MessageDialog.openError(getShell(), WizardMessages.NewTestSuiteWizPage_cannotUpdateDialog_title, 
			Messages.format(WizardMessages.NewTestSuiteWizPage_cannotUpdateDialog_message, new String[] { NewTestSuiteWizardPage.START_MARKER, NewTestSuiteWizardPage.END_MARKER})); 

	}

	protected void postCreatingType() {
		IType newClass= fPage.getCreatedType();
		if (newClass == null)
			return;
		ICompilationUnit cu= newClass.getCompilationUnit();
		IResource resource= cu.getResource();
		if (resource != null) {
			selectAndReveal(resource);
			openResource(resource);
		}
	}

	public NewTestSuiteWizardPage getPage() {
		return fPage;
	}
	
	protected void initializeDefaultPageImageDescriptor() {
		setDefaultPageImageDescriptor(JUnitPlugin.getImageDescriptor("wizban/newtest_wiz.png")); //$NON-NLS-1$
	}

	public IRunnableWithProgress getRunnableSave(final IEditorPart cu_ep) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					if (monitor == null) {
						monitor= new NullProgressMonitor();
					}
					cu_ep.doSave(monitor);
			}
		};
	}
}
