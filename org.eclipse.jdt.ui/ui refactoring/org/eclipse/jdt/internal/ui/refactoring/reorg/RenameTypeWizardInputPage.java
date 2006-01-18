/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.ISimilarDeclarationUpdating;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Wizard page for renaming a type (with similarly named elements)
 * 
 * @since 3.2
 * 
 */
class RenameTypeWizardInputPage extends RenameInputWizardPage {

	private Button fUpdateSimilarElements;
	private int fSelectedStrategy;

	private static final String UPDATE_SIMILAR_ELEMENTS= "updateSimilarElements"; //$NON-NLS-1$
	private final static String DIALOG_SETTINGS_SIMILAR_MATCH_STRATEGY= "updateSimilarElementsMatchStrategy"; //$NON-NLS-1$
	private Button fUpdateSimilarElementsButton;

	public RenameTypeWizardInputPage(String description, String contextHelpId, boolean isLastUserPage, String initialValue) {
		super(description, contextHelpId, isLastUserPage, initialValue);
	}

	protected void addAdditionalOptions(Composite parent) {

		if (getSimilarElementUpdating() == null || !getSimilarElementUpdating().canEnableSimilarDeclarationUpdating())
			return;

		try {
			fSelectedStrategy= getRefactoringSettings().getInt(DIALOG_SETTINGS_SIMILAR_MATCH_STRATEGY);
		} catch (NumberFormatException e) {
			fSelectedStrategy= getSimilarElementUpdating().getMatchStrategy();
		}

		getSimilarElementUpdating().setMatchStrategy(fSelectedStrategy);

		final Composite composite= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout(2, false);
		layout.marginHeight= 0;
		composite.setLayout(layout);
		GridData data= new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalIndent= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
		composite.setLayoutData(data);

		fUpdateSimilarElements= new Button(composite, SWT.CHECK);
		fUpdateSimilarElements.setText(RefactoringMessages.RenameTypeWizardInputPage_update_similar_elements);

		final boolean updateSimilarElements= getBooleanSetting(UPDATE_SIMILAR_ELEMENTS, getSimilarElementUpdating().getUpdateSimilarDeclarations());
		fUpdateSimilarElements.setSelection(updateSimilarElements);
		getSimilarElementUpdating().setUpdateSimilarDeclarations(updateSimilarElements);
		fUpdateSimilarElements.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUpdateSimilarElements.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				getSimilarElementUpdating().setUpdateSimilarDeclarations(fUpdateSimilarElements.getSelection());
				fUpdateSimilarElementsButton.setEnabled(fUpdateSimilarElements.getSelection());
			}
		});

		fUpdateSimilarElementsButton= new Button(composite, SWT.PUSH);
		data= new GridData();
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= SWT.RIGHT;
		fUpdateSimilarElementsButton.setText(RefactoringMessages.RenameTypeWizardInputPage_update_similar_elements_configure);
		fUpdateSimilarElementsButton.setEnabled(updateSimilarElements);
		fUpdateSimilarElementsButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				RenameTypeWizardSimilarElementsOptionsDialog dialog= new RenameTypeWizardSimilarElementsOptionsDialog(getShell(), fSelectedStrategy);
				if (dialog.open() == Window.OK) {
					fSelectedStrategy= dialog.getSelectedStrategy();
					getSimilarElementUpdating().setMatchStrategy(fSelectedStrategy);
				}
			}
		});
		fUpdateSimilarElementsButton.setLayoutData(data);

		data= new GridData();
		data.grabExcessHorizontalSpace= true;
		data.horizontalAlignment= SWT.FILL;
		data.horizontalSpan= 2;
		composite.setLayoutData(data);
	}

	public void dispose() {
		if (saveSettings())
			if (fUpdateSimilarElements != null && !fUpdateSimilarElements.isDisposed() && fUpdateSimilarElements.isEnabled()) {
				saveBooleanSetting(UPDATE_SIMILAR_ELEMENTS, fUpdateSimilarElements);
				getRefactoringSettings().put(DIALOG_SETTINGS_SIMILAR_MATCH_STRATEGY, fSelectedStrategy);
			}

		super.dispose();
	}

	/*
	 * Override - we don't want to initialize the next page (may needlessly
	 * trigger change creation if similar elements page is skipped, which is not
	 * indicated by fIsLastUserInputPage in parent).
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	private ISimilarDeclarationUpdating getSimilarElementUpdating() {
		return (ISimilarDeclarationUpdating) getRefactoring().getAdapter(ISimilarDeclarationUpdating.class);
	}
	
	protected boolean performFinish() {
		boolean returner= super.performFinish();
		// check if we got deferred to the error page
		if (!returner && getContainer().getCurrentPage() != null)
			getContainer().getCurrentPage().setPreviousPage(this);
		return returner;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.ui.refactoring.UserInputWizardPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		RenameTypeWizard wizard= (RenameTypeWizard) getWizard();
		IWizardPage nextPage;
		
		if (wizard.isRenameType()) {
			final RenameTypeProcessor renameTypeProcessor= wizard.getRenameTypeProcessor();
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {

					public void run(IProgressMonitor pm) throws InterruptedException {
						try {
							renameTypeProcessor.initializeReferences(pm);
						} catch (OperationCanceledException e) {
							throw new InterruptedException();
						} catch (CoreException e) {
							ExceptionHandler.handle(e, RefactoringMessages.RenameTypeWizard_defaultPageTitle,
									RefactoringMessages.RenameTypeWizard_unexpected_exception);
						} finally {
							pm.done();
						}
					}
				});
			} catch (InvocationTargetException e) {
				ExceptionHandler.handle(e, getShell(), RefactoringMessages.RenameTypeWizard_defaultPageTitle,
						RefactoringMessages.RenameTypeWizard_unexpected_exception);
			} catch (InterruptedException e) {
				// user canceled
				return this;
			}

			if (renameTypeProcessor.hasSimilarElementsToRename()) {
				nextPage= super.getNextPage();
			} else {
				nextPage= computeSuccessorPage();
			}
			
		} else
			nextPage= computeSuccessorPage();
		
		nextPage.setPreviousPage(this);
		return nextPage;
	}
}
