/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.refactoring.RefactoringAvailabilityTester;
import org.eclipse.jdt.internal.corext.refactoring.RefactoringExecutionStarter;
import org.eclipse.jdt.internal.corext.refactoring.reorg.ReorgUtilsCore;

import org.eclipse.jdt.ui.actions.SelectionDispatchAction;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.refactoring.RefactoringMessages;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;

public class CutAction extends SelectionDispatchAction{

	private final CopyToClipboardAction fCopyToClipboardAction;

	public CutAction(IWorkbenchSite site) {
		super(site);
		setText(ReorgMessages.CutAction_text);
		fCopyToClipboardAction= new CopyToClipboardAction(site);

		ISharedImages workbenchImages= PlatformUI.getWorkbench().getSharedImages();
		setDisabledImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
		setImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		setHoverImageDescriptor(workbenchImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CUT_ACTION);
	}

	@Override
	public void selectionChanged(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			// cannot cut top-level types. this deletes the cu and then you cannot paste because the cu is gone.
			if (!containsOnlyElementsInsideCompilationUnits(selection) || containsTopLevelTypes(selection)) {
				setEnabled(false);
				return;
			}
			fCopyToClipboardAction.selectionChanged(selection);
			setEnabled(fCopyToClipboardAction.isEnabled() && RefactoringAvailabilityTester.isDeleteAvailable(selection));
		} else
			setEnabled(false);
	}

	private static boolean containsOnlyElementsInsideCompilationUnits(IStructuredSelection selection) {
		for (Object object : selection) {
			if (!(object instanceof IJavaElement)
					|| !ReorgUtilsCore.isInsideCompilationUnit((IJavaElement)object))
				return false;
		}
		return true;
	}

	private static boolean containsTopLevelTypes(IStructuredSelection selection) {
		for (Object each : selection) {
			if (each instanceof IType && ((IType)each).getDeclaringType() == null)
				return true;
		}
		return false;
	}

	@Override
	public void run(IStructuredSelection selection) {
		try {
			selectionChanged(selection);
			if (isEnabled()) {
				fCopyToClipboardAction.run(selection);
				RefactoringExecutionStarter.startCutRefactoring(selection.toArray(), getShell());
			}
		} catch (InterruptedException e) {
			//OK
		} catch (InvocationTargetException e) {
			ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.OpenRefactoringWizardAction_exception);
		}
	}
}
