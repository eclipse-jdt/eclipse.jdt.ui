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
package org.eclipse.jdt.internal.ui.refactoring;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.structure.ExtractSupertypeRefactoring;

import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

/**
 * Wizard page for the extract supertype refactoring, which, apart from pull up
 * facilities, also allows to specify the types where to extract the supertype.
 * 
 * @since 3.2
 */
public final class ExtractSupertypeMemberPage extends PullUpMemberPage {

	/**
	 * Creates a new extract supertype member page.
	 * 
	 * @param page
	 *            the method page
	 */
	public ExtractSupertypeMemberPage(final PullUpMethodPage page) {
		super(page);
		setMessage(RefactoringMessages.ExtractSupertypeMemberPage_page_title);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void createSuperTypeControl(final Composite parent) {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().run(false, false, new IRunnableWithProgress() {

				public void run(final IProgressMonitor monitor) throws InvocationTargetException {
					try {
						createSuperTypeList(monitor, parent);
					} catch (JavaModelException exception) {
						throw new InvocationTargetException(exception);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (InvocationTargetException exception) {
			ExceptionHandler.handle(exception, getShell(), RefactoringMessages.ExtractSupertypeMemberPage_extract_supertype, RefactoringMessages.PullUpInputPage_exception);
		} catch (InterruptedException exception) {
			Assert.isTrue(false);
		}
	}

	/**
	 * Creates the super type list.
	 * 
	 * @param monitor
	 *            the progress monitor to use
	 * @param parent
	 *            the parent control
	 */
	protected void createSuperTypeList(final IProgressMonitor monitor, final Composite parent) throws JavaModelException {
		fCandidateTypes= getExtractSuperTypeRefactoring().getExtractSupertypeProcessor().getCandidateTypes(new RefactoringStatus(), monitor);
		// TODO: implement
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getCreateStubsButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_create_stubs_label;
	}

	/**
	 * Returns the extract supertype refactoring.
	 */
	public ExtractSupertypeRefactoring getExtractSuperTypeRefactoring() {
		return (ExtractSupertypeRefactoring) getRefactoring();
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getInstanceofButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_use_supertype_label;
	}

	/**
	 * {@inheritDoc}
	 */
	protected String getReplaceButtonLabel() {
		return RefactoringMessages.ExtractSupertypeMemberPage_use_instanceof_label;
	}
}