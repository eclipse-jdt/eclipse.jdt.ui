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
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenameTypeProcessor;
import org.eclipse.jdt.internal.corext.refactoring.tagging.IDerivedElementUpdating;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.RowLayouter;

/**
 * Wizard page for renaming a type (with derived elements)
 * 
 * @since 3.2
 * 
 */
class RenameTypeWizardInputPage extends RenameInputWizardPage {

	private Button fUpdateDerivedElements;
	private int fSelectedStrategy;

	private static final String UPDATE_DERIVED_ELEMENTS= "updateDerivedElements"; //$NON-NLS-1$
	private final static String DIALOG_SETTINGS_DERIVED_MATCH_STRATEGY= "updateDerivedElementsMatchStrategy"; //$NON-NLS-1$
	private Button fUpdateDerivedElementsButton;

	public RenameTypeWizardInputPage(String description, String contextHelpId, boolean isLastUserPage, String initialValue) {
		super(description, contextHelpId, isLastUserPage, initialValue);
	}

	protected void addAdditionalOptions(Composite composite, RowLayouter layouter) {

		if (getDerivedElementUpdating() == null || !getDerivedElementUpdating().canEnableDerivedElementUpdating())
			return;

		try {
			fSelectedStrategy= getRefactoringSettings().getInt(DIALOG_SETTINGS_DERIVED_MATCH_STRATEGY);
		} catch (NumberFormatException e) {
			fSelectedStrategy= getDerivedElementUpdating().getMatchStrategy();
		}

		getDerivedElementUpdating().setMatchStrategy(fSelectedStrategy);

		Composite c= new Composite(composite, SWT.NULL);
		GridLayout layout= new GridLayout(2, false);
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		c.setLayout(layout);

		fUpdateDerivedElements= new Button(c, SWT.CHECK);
		fUpdateDerivedElements.setText(RefactoringMessages.RenameTypeWizardInputPage_update_derived_elements);

		final boolean updateDerivedElements= getBooleanSetting(UPDATE_DERIVED_ELEMENTS, getDerivedElementUpdating().getUpdateDerivedElements());
		fUpdateDerivedElements.setSelection(updateDerivedElements);
		getDerivedElementUpdating().setUpdateDerivedElements(updateDerivedElements);
		fUpdateDerivedElements.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fUpdateDerivedElements.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				getDerivedElementUpdating().setUpdateDerivedElements(fUpdateDerivedElements.getSelection());
				fUpdateDerivedElementsButton.setEnabled(fUpdateDerivedElements.getSelection());
			}
		});

		fUpdateDerivedElementsButton= new Button(c, SWT.PUSH);
		GridData d= new GridData();
		d.grabExcessHorizontalSpace= true;
		d.horizontalAlignment= SWT.RIGHT;
		fUpdateDerivedElementsButton.setText(RefactoringMessages.RenameTypeWizardInputPage_update_derived_elements_configure);
		fUpdateDerivedElementsButton.setEnabled(updateDerivedElements);
		fUpdateDerivedElementsButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				RenameTypeWizardDerivedOptionsDialog dialog= new RenameTypeWizardDerivedOptionsDialog(getShell(), fSelectedStrategy);
				if (dialog.open() == Window.OK) {
					fSelectedStrategy= dialog.getSelectedStrategy();
					getDerivedElementUpdating().setMatchStrategy(fSelectedStrategy);
				}
			}
		});
		fUpdateDerivedElementsButton.setLayoutData(d);

		GridData forC= new GridData();
		forC.grabExcessHorizontalSpace= true;
		forC.horizontalAlignment= SWT.FILL;
		forC.horizontalSpan= 2;
		c.setLayoutData(forC);

		final Label separator= new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		layouter.perform(separator);
	}

	public void dispose() {
		if (saveSettings())
			if (fUpdateDerivedElements != null && !fUpdateDerivedElements.isDisposed() && fUpdateDerivedElements.isEnabled()) {
				saveBooleanSetting(UPDATE_DERIVED_ELEMENTS, fUpdateDerivedElements);
				getRefactoringSettings().put(DIALOG_SETTINGS_DERIVED_MATCH_STRATEGY, fSelectedStrategy);
			}

		super.dispose();
	}

	/*
	 * Override - we don't want to initialize the next page (may needlessly
	 * trigger change creation if derived page is skipped, which is not
	 * indicated by fIsLastUserInputPage in parent).
	 */
	public boolean canFlipToNextPage() {
		return isPageComplete();
	}

	private IDerivedElementUpdating getDerivedElementUpdating() {
		return (IDerivedElementUpdating) getRefactoring().getAdapter(IDerivedElementUpdating.class);
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

			if (renameTypeProcessor.hasDerivedElementsToRename()) {
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
