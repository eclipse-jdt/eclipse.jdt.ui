/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.fix;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;

import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import org.eclipse.ui.PlatformUI;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation;
import org.eclipse.jdt.core.manipulation.OrganizeImportsOperation.IChooseImportQuery;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.internal.core.manipulation.util.BasicElementLabels;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;
import org.eclipse.jdt.internal.corext.util.Messages;

import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

import org.eclipse.jdt.internal.ui.actions.ActionMessages;

public class ImportsFix extends TextEditFix {

	public static ICleanUpFix createCleanUp(final CompilationUnit cu, CodeGenerationSettings settings, boolean organizeImports, RefactoringStatus status) throws CoreException {
		if (!organizeImports)
			return null;

		final boolean hasAmbiguity[]= new boolean[] { false };
		IChooseImportQuery query= (openChoices, ranges) -> {
			hasAmbiguity[0]= true;
			return new TypeNameMatch[0];
		};

		final ICompilationUnit unit= (ICompilationUnit)cu.getJavaElement();
		OrganizeImportsOperation op= new OrganizeImportsOperation(unit, cu, settings.importIgnoreLowercase, false, false, query);

		TextEdit edit;

		try {
			edit= runUsingProgressMonitorDialog(op);
		} catch (OperationCanceledException e) {
			// The user canceled. No need to propagate, simply abort the "organize imports" operation
			return null;
		}

		if (hasAmbiguity[0]) {
			status.addInfo(Messages.format(ActionMessages.OrganizeImportsAction_multi_error_unresolvable, getLocationString(cu)));
		}

		if (op.getParseError() != null) {
			status.addInfo(Messages.format(ActionMessages.OrganizeImportsAction_multi_error_parse, getLocationString(cu)));
			return null;
		}

		if (edit == null || (edit instanceof MultiTextEdit && edit.getChildrenSize() == 0))
			return null;

		return new ImportsFix(edit, unit, FixMessages.ImportsFix_OrganizeImports_Description);
    }

	private static TextEdit runUsingProgressMonitorDialog(OrganizeImportsOperation op) throws CoreException {

		final AtomicReference<TextEdit> edit= new AtomicReference<>();
		Shell shell= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();

		ProgressMonitorDialog dialog= new ProgressMonitorDialog(shell);
		try {
			dialog.run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						edit.set(op.createTextEdit(monitor));
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		} catch (InvocationTargetException e) {
			// CoreExceptions are re-thrown
			if (e.getCause() instanceof CoreException ce)
				throw ce;

			// Other kind of exceptions are packed into a CoreException
			throw new CoreException(Status.error(e.getCause().getMessage(), e.getCause()));
		} catch (InterruptedException e) {
			throw new OperationCanceledException();
		}

		return edit.get();
	}

	private static String getLocationString(final CompilationUnit cu) {
		return BasicElementLabels.getPathLabel(cu.getJavaElement().getPath(), false);
	}

	public ImportsFix(TextEdit edit, ICompilationUnit unit, String description) {
		super(edit, unit, description);
	}
}
