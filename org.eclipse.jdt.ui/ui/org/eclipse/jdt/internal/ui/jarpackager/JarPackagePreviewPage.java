/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.core.resources.IProject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.RefactoringUI;
import org.eclipse.ltk.ui.refactoring.history.IRefactoringHistoryControl;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryControlConfiguration;

import org.eclipse.jdt.internal.corext.Assert;

import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Jar export wizard page to preview refactorings.
 * 
 * @since 3.2
 */
public class JarPackagePreviewPage extends WizardPage implements IJarPackageWizardPage {

	/** The page name */
	private final static String PAGE_NAME= "jarPreviewWizardPage"; //$NON-NLS-1$

	/** The refactoring history control configuration to use */
	protected final RefactoringHistoryControlConfiguration fControlConfiguration;

	/** The refactoring history control */
	protected IRefactoringHistoryControl fHistoryControl= null;

	/** The jar package data */
	protected final JarPackageData fJarPackageData;

	/**
	 * Creates a new jar package preview page.
	 * 
	 * @param data
	 *            the jar package data
	 * @param configuration
	 *            the refactoring history control configuration to use
	 */
	public JarPackagePreviewPage(final JarPackageData data, final RefactoringHistoryControlConfiguration configuration) {
		super(PAGE_NAME);
		Assert.isNotNull(configuration);
		Assert.isNotNull(data);
		fControlConfiguration= configuration;
		fJarPackageData= data;
		setTitle(JarPackagerMessages.RefactoringPreviewPage_title);
		setDescription(JarPackagerMessages.RefactoringPreviewPage_description);
	}

	/**
	 * {@inheritDoc}
	 */
	public void createControl(final Composite parent) {
		initializeDialogUnits(parent);
		final Composite composite= new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		fHistoryControl= (IRefactoringHistoryControl) RefactoringUI.createRefactoringHistoryControl(composite, fControlConfiguration);
		fHistoryControl.createControl();
		setControl(composite);
		Dialog.applyDialogFont(composite);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IJavaHelpContextIds.JARPREVIEW_WIZARD_PAGE);
	}

	/**
	 * {@inheritDoc}
	 */
	public void finish() {
		// Do nothing
	}

	/**
	 * Retrieves the refactoring history to preview.
	 * 
	 * @return the refactoring history, or <code>null</code>
	 */
	protected RefactoringHistory retrieveRefactoringHistory() {
		final RefactoringHistory[] history= { null};
		final IProject[] projects= fJarPackageData.getRefactoringProjects();
		try {
			getContainer().run(false, true, new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					history[0]= JarPackagerUtil.retrieveHistory(projects, 0, Long.MAX_VALUE, fJarPackageData.isExportStructuralOnly() ? RefactoringDescriptor.STRUCTURAL_CHANGE : RefactoringDescriptor.NONE, monitor);
				}
			});
		} catch (InvocationTargetException exception) {
			JavaPlugin.log(exception);
		} catch (InterruptedException exception) {
			// Just interrupted
		}
		return history[0];
	}

	/**
	 * {@inheritDoc}
	 */
	public final void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && fJarPackageData.isRefactoringAware()) {
			final RefactoringHistory history= retrieveRefactoringHistory();
			if (history != null) {
				fHistoryControl.setInput(history);
				fHistoryControl.setCheckedDescriptors(fJarPackageData.getRefactoringDescriptors());
			}
		} else
			fJarPackageData.setRefactoringDescriptors(new RefactoringDescriptorProxy[0]);
	}
}