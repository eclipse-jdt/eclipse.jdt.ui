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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaMoveProcessor;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.refactoring.actions.RefactoringStarter;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.participants.MoveRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;


public class ReorgMoveStarter {
	private final JavaMoveProcessor fMoveProcessor;

	private ReorgMoveStarter(JavaMoveProcessor moveProcessor) {
		Assert.isNotNull(moveProcessor);
		fMoveProcessor= moveProcessor;
	}
	
	public static ReorgMoveStarter create(IJavaElement[] javaElements, IResource[] resources, IJavaElement destination) throws JavaModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		JavaMoveProcessor processor= JavaMoveProcessor.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
		if (processor == null)
			return null;
		if (! processor.setDestination(destination).isOK())
			return null;
		return new ReorgMoveStarter(processor);
	}

	public static ReorgMoveStarter create(IJavaElement[] javaElements, IResource[] resources, IResource destination) throws JavaModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		JavaMoveProcessor processor= JavaMoveProcessor.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
		if (processor == null)
			return null;
		if (! processor.setDestination(destination).isOK())
			return null;
		return new ReorgMoveStarter(processor);
	}
	
	public void run(Shell parent) throws InterruptedException, InvocationTargetException {
		try {
			MoveRefactoring ref= new MoveRefactoring(fMoveProcessor);
			if (fMoveProcessor.hasAllInputSet()) {
				IRunnableContext context= new ProgressMonitorDialog(parent);
				fMoveProcessor.setCreateTargetQueries(new CreateTargetQueries(parent));
				fMoveProcessor.setReorgQueries(new ReorgQueries(parent));
				new RefactoringExecutionHelper(ref, RefactoringCore.getConditionCheckingFailedSeverity(), true, parent, context).perform();
			} else  {
				RefactoringWizard wizard= new ReorgMoveWizard(ref);
				/*
				 * We want to get the shell from the refactoring dialog but it's not known at this point, 
				 * so we pass the wizard and then, once the dialog is open, we will have access to its shell.
				 */
				fMoveProcessor.setCreateTargetQueries(new CreateTargetQueries(wizard));
				fMoveProcessor.setReorgQueries(new ReorgQueries(wizard));
				new RefactoringStarter().activate(ref, wizard, parent, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), true); //$NON-NLS-1$
			}
		} catch (JavaModelException e) {
			ExceptionHandler.handle(e, RefactoringMessages.getString("OpenRefactoringWizardAction.refactoring"), RefactoringMessages.getString("OpenRefactoringWizardAction.exception")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
}
