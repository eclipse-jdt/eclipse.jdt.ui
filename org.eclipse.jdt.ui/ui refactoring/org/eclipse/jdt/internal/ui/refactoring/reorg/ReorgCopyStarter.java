/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
import org.eclipse.jdt.internal.corext.refactoring.reorg.CopyRefactoring;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;

import org.eclipse.ltk.core.refactoring.RefactoringCore;

public class ReorgCopyStarter {
	
	private final CopyRefactoring fCopyRefactoring;

	private ReorgCopyStarter(CopyRefactoring copyRefactoring) {
		Assert.isNotNull(copyRefactoring);
		fCopyRefactoring= copyRefactoring;
	}
	
	public static ReorgCopyStarter create(IJavaElement[] javaElements, IResource[] resources, IJavaElement destination) throws JavaModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		CopyRefactoring copyRefactoring= CopyRefactoring.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
		if (copyRefactoring == null)
			return null;
		if (! copyRefactoring.setDestination(destination).isOK())
			return null;
		return new ReorgCopyStarter(copyRefactoring);
	}

	public static ReorgCopyStarter create(IJavaElement[] javaElements, IResource[] resources, IResource destination) throws JavaModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		CopyRefactoring copyRefactoring= CopyRefactoring.create(resources, javaElements, JavaPreferencesSettings.getCodeGenerationSettings());
		if (copyRefactoring == null)
			return null;
		if (! copyRefactoring.setDestination(destination).isOK())
			return null;
		return new ReorgCopyStarter(copyRefactoring);
	}
	
	public void run(Shell parent) throws InterruptedException, InvocationTargetException {
		IRunnableContext context= new ProgressMonitorDialog(parent);
		fCopyRefactoring.setNewNameQueries(new NewNameQueries(parent));
		fCopyRefactoring.setReorgQueries(new ReorgQueries(parent));
		new RefactoringExecutionHelper(fCopyRefactoring, RefactoringCore.getConditionCheckingFailedSeverity(), false, parent, context).perform();
	}
}