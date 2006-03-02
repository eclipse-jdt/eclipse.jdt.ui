/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.deprecation;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;

import org.eclipse.jface.dialogs.IDialogSettings;

import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptorProxy;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.history.RefactoringHistory;
import org.eclipse.ltk.ui.refactoring.history.RefactoringHistoryControlConfiguration;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;

import org.eclipse.jdt.internal.corext.refactoring.JavaRefactoringDescriptor;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.refactoring.binary.StubRefactoringHistoryWizard;

/**
 * Refactoring wizard to fix deprecations using a refactoring script.
 * 
 * @since 3.2
 */
public final class FixDeprecationRefactoringWizard extends StubRefactoringHistoryWizard {

	/** Proxy which encapsulates a refactoring history */
	private final class RefactoringHistoryProxy extends RefactoringHistory {

		/**
		 * {@inheritDoc}
		 */
		public RefactoringDescriptorProxy[] getDescriptors() {
			if (fRefactoringHistory != null)
				return fRefactoringHistory.getDescriptors();
			return new RefactoringDescriptorProxy[0];
		}

		/**
		 * {@inheritDoc}
		 */
		public boolean isEmpty() {
			final RefactoringDescriptorProxy[] proxies= getDescriptors();
			if (proxies != null)
				return proxies.length == 0;
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		public RefactoringHistory removeAll(final RefactoringHistory history) {
			throw new UnsupportedOperationException();
		}
	}

	/** The dialog settings key */
	private static String DIALOG_SETTINGS_KEY= "FixDeprecationWizard"; //$NON-NLS-1$

	/** The compilation unit */
	private final ICompilationUnit fCompilationUnit;

	/** The selection length */
	private final int fLength;

	/** Has the wizard new dialog settings? */
	private boolean fNewSettings;

	/** The selection offset */
	private final int fOffset;

	/** The package fragment root, or <code>null</code> */
	private IPackageFragmentRoot fPackageFragmentRoot;

	/** The refactoring history, or <code>null</code> */
	private RefactoringHistory fRefactoringHistory= null;

	/**
	 * Creates a new fix deprecation refactoring wizard.
	 * 
	 * @param unit
	 *            the compilation unit
	 * @param offset
	 *            the selection offset
	 * @param length
	 *            the selection length
	 */
	public FixDeprecationRefactoringWizard(final ICompilationUnit unit, final int offset, final int length) {
		super(false, DeprecationMessages.FixDeprecationRefactoringWizard_caption, DeprecationMessages.FixDeprecationRefactoringWizard_title, DeprecationMessages.FixDeprecationRefactoringWizard_description);
		Assert.isNotNull(unit);
		Assert.isTrue(offset >= 0);
		Assert.isTrue(length >= 0);
		fCompilationUnit= unit;
		fOffset= offset;
		fLength= length;
		setInput(new RefactoringHistoryProxy());
		setDefaultPageImageDescriptor(JavaPluginImages.DESC_WIZBAN_REFACTOR);
		final IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
		final IDialogSettings section= settings.getSection(DIALOG_SETTINGS_KEY);
		if (section == null)
			fNewSettings= true;
		else {
			fNewSettings= false;
			setDialogSettings(section);
		}
		setConfiguration(new RefactoringHistoryControlConfiguration(null, false, false) {

			public String getProjectPattern() {
				return DeprecationMessages.FixDeprecationRefactoringWizard_project_pattern;
			}

			public String getWorkspaceCaption() {
				return DeprecationMessages.FixDeprecationRefactoringWizard_workspace_caption;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean canFinish() {
		return super.canFinish() && fRefactoringHistory != null;
	}

	/**
	 * {@inheritDoc}
	 */
	protected Refactoring createRefactoring(final RefactoringDescriptor descriptor, final RefactoringStatus status) throws CoreException {
		if (descriptor instanceof JavaRefactoringDescriptor) {
			final JavaRefactoringDescriptor extended= (JavaRefactoringDescriptor) descriptor;
			String project= extended.getProject();
			final Map arguments= new HashMap(extended.getArguments());
			final String handle= (String) arguments.get(JavaRefactoringDescriptor.INPUT);
			if (handle != null && !"".equals(handle)) { //$NON-NLS-1$
				final IJavaElement element= JavaRefactoringDescriptor.handleToElement(project, handle);
				if (element != null) {
					if (element instanceof ICompilationUnit)
						arguments.put(JavaRefactoringDescriptor.INPUT, JavaRefactoringDescriptor.elementToHandle(project, fCompilationUnit));
					project= fCompilationUnit.getJavaProject().getElementName();
				}
			}
			final String selection= (String) arguments.get(JavaRefactoringDescriptor.SELECTION);
			if (selection != null) {
				final StringBuffer buffer= new StringBuffer(8);
				buffer.append(fOffset);
				buffer.append(' ');
				buffer.append(fLength);
				arguments.put(JavaRefactoringDescriptor.SELECTION, buffer.toString());
			}
			return super.createRefactoring(new JavaRefactoringDescriptor(extended.getID(), project, extended.getDescription(), extended.getComment(), arguments, extended.getFlags()), status);
		}
		return super.createRefactoring(descriptor, status);
	}

	/**
	 * {@inheritDoc}
	 */
	public IPackageFragmentRoot getPackageFragmentRoot() {
		return fPackageFragmentRoot;
	}

	/**
	 * Returns the refactoring history to apply.
	 * 
	 * @return the refactoring history to apply, or <code>null</code>
	 */
	public RefactoringHistory getRefactoringHistory() {
		return fRefactoringHistory;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean performFinish() {
		final boolean result= super.performFinish();
		if (fNewSettings) {
			final IDialogSettings settings= JavaPlugin.getDefault().getDialogSettings();
			IDialogSettings section= settings.getSection(DIALOG_SETTINGS_KEY);
			section= settings.addNewSection(DIALOG_SETTINGS_KEY);
			setDialogSettings(section);
		}
		return result;
	}

	/**
	 * Sets the package fragment root.
	 * 
	 * @param root
	 *            the package fragment root
	 */
	public void setPackageFragmentRoot(final IPackageFragmentRoot root) {
		fPackageFragmentRoot= root;
	}

	/**
	 * Sets the refactoring history to apply.
	 * 
	 * @param history
	 *            the refactoring history to apply, or <code>null</code>
	 */
	public void setRefactoringHistory(final RefactoringHistory history) {
		fRefactoringHistory= history;
	}
}