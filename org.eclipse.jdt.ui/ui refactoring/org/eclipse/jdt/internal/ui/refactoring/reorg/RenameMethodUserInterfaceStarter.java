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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IMethod;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.dialogs.MessageDialog;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringCoreMessages;
import org.eclipse.jdt.internal.corext.refactoring.rename.RenameVirtualMethodProcessor;
import org.eclipse.jdt.internal.corext.refactoring.util.JavaElementUtil;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class RenameMethodUserInterfaceStarter extends RenameUserInterfaceStarter {
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.refactoring.reorg.RenameUserInterfaceStarter#activate(org.eclipse.jdt.internal.corext.refactoring.base.Refactoring, org.eclipse.swt.widgets.Shell)
	 */
	public void activate(Refactoring refactoring, Shell parent, boolean save) throws CoreException {
		RenameVirtualMethodProcessor processor= (RenameVirtualMethodProcessor)refactoring.getAdapter(RenameVirtualMethodProcessor.class);
		if (processor != null) {
			RefactoringStatus status= processor.checkInitialConditions(new NullProgressMonitor());
			if (!status.hasFatalError()) {
				IMethod method= processor.getMethod();
				if (!method.equals(processor.getOriginalMethod())) {
					String message= null;
					if (method.getDeclaringType().isInterface()) {
						message= RefactoringCoreMessages.getFormattedString(
							"MethodChecks.implements", //$NON-NLS-1$
							new String[]{
								JavaElementUtil.createMethodSignature(method), 
								JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
					} else {
						message= RefactoringCoreMessages.getFormattedString(
							"MethodChecks.overrides", //$NON-NLS-1$
							new String[]{
								JavaElementUtil.createMethodSignature(method), 
								JavaModelUtil.getFullyQualifiedName(method.getDeclaringType())});
					}
					message= ReorgMessages.getFormattedString(
						"RenameMethodUserInterfaceStarter.message",  //$NON-NLS-1$
						message);
					if (!MessageDialog.openQuestion(parent, 
							ReorgMessages.getString("RenameMethodUserInterfaceStarter.name"),  //$NON-NLS-1$
							message)) {
						return;
					}
				}
			}
		}
		super.activate(refactoring, parent, save);
	}
}
