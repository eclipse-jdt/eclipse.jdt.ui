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

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;

import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.participants.CopyRefactoring;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.reorg.JavaCopyProcessor;

import org.eclipse.jdt.internal.ui.refactoring.RefactoringExecutionHelper;

public class ReorgCopyStarter {
	
	private final JavaCopyProcessor fCopyProcessor;

	private ReorgCopyStarter(JavaCopyProcessor copyProcessor) {
		Assert.isNotNull(copyProcessor);
		fCopyProcessor= copyProcessor;
	}
	
	public static ReorgCopyStarter create(IJavaElement[] javaElements, IResource[] resources, IJavaElement destination) throws JavaModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		JavaCopyProcessor copyProcessor= JavaCopyProcessor.create(resources, javaElements);
		if (copyProcessor == null)
			return null;
		if (! copyProcessor.setDestination(destination).isOK())
			return null;
		return new ReorgCopyStarter(copyProcessor);
	}

	public static ReorgCopyStarter create(IJavaElement[] javaElements, IResource[] resources, IResource destination) throws JavaModelException {
		Assert.isNotNull(javaElements);
		Assert.isNotNull(resources);
		Assert.isNotNull(destination);
		JavaCopyProcessor copyProcessor= JavaCopyProcessor.create(resources, javaElements);
		if (copyProcessor == null)
			return null;
		if (! copyProcessor.setDestination(destination).isOK())
			return null;
		return new ReorgCopyStarter(copyProcessor);
	}
	
	public void run(Shell parent) throws InterruptedException, InvocationTargetException {
		IRunnableContext context= new ProgressMonitorDialog(parent);
		fCopyProcessor.setNewNameQueries(new NewNameQueries(parent));
		fCopyProcessor.setReorgQueries(new ReorgQueries(parent));
		new RefactoringExecutionHelper(new CopyRefactoring(fCopyProcessor), RefactoringCore.getConditionCheckingFailedSeverity(), false, parent, context).perform();
	}
}
