/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Vector Informatik GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.swt.widgets.Display;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

/**
 * Test clean up that does nothing unless armed via {@link #arm(String, String, Runnable)} or
 * {@link #armInSeparatePasses(Replacement...)}. When armed, it replaces the first occurrence of a
 * text and runs an action in the UI thread after calculating the offset of the replacement.
 * Afterwards, it reads the text at that offset again and returns the fix.
 */
public class PausingCleanUp implements ICleanUp {

	/**
	 * A replacement done by the clean up.
	 *
	 * @param oldText the text to replace
	 * @param newText the replacement
	 * @param actionInUIThread the action to run in the UI thread while the calculation is paused, or
	 *            <code>null</code> to not pause
	 */
	public record Replacement(String oldText, String newText, Runnable actionInUIThread) {
	}

	private static final Queue<Replacement> fgReplacements= new ConcurrentLinkedQueue<>();

	private static volatile boolean fgSeparatePasses;

	/**
	 * Arms the clean up for the next calculation.
	 *
	 * @param oldText the text to replace
	 * @param newText the replacement
	 * @param actionInUIThread the action to run in the UI thread while the calculation is paused
	 */
	public static void arm(String oldText, String newText, Runnable actionInUIThread) {
		disarm();
		fgReplacements.add(new Replacement(oldText, newText, actionInUIThread));
	}

	/**
	 * Arms the clean up so that the given replacements are calculated and applied in separate passes,
	 * in the given order. Each registered instance of this clean up does at most one replacement per
	 * pass.
	 *
	 * @param replacements the replacements, at most as many as registered instances of this clean up
	 */
	public static void armInSeparatePasses(Replacement... replacements) {
		disarm();
		fgReplacements.addAll(List.of(replacements));
		fgSeparatePasses= true;
	}

	public static void disarm() {
		fgSeparatePasses= false;
		fgReplacements.clear();
	}

	@Override
	public void setOptions(CleanUpOptions options) {
	}

	@Override
	public String[] getStepDescriptions() {
		return new String[0];
	}

	@Override
	public CleanUpRequirements getRequirements() {
		// Requiring a fresh AST makes this clean up run in a separate pass if another one ran before it
		boolean separatePasses= fgSeparatePasses;
		return new CleanUpRequirements(separatePasses, separatePasses, false, null);
	}

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
		Replacement replacement= fgReplacements.poll();
		if (replacement == null) {
			return null;
		}

		ICompilationUnit unit= context.getCompilationUnit();
		int offset= unit.getSource().indexOf(replacement.oldText());
		if (offset == -1) {
			return null;
		}

		if (replacement.actionInUIThread() != null) {
			Display.getDefault().syncExec(replacement.actionInUIThread());
		}

		// Like real clean ups, keep reading the document at the calculated offset
		unit.getBuffer().getText(offset, replacement.oldText().length());

		return new ICleanUpFix() {
			@Override
			public CompilationUnitChange createChange(IProgressMonitor progressMonitor) throws CoreException {
				CompilationUnitChange change= new CompilationUnitChange(PausingCleanUp.class.getSimpleName(), unit);
				change.setEdit(new ReplaceEdit(offset, replacement.oldText().length(), replacement.newText()));
				return change;
			}
		};
	}

	@Override
	public RefactoringStatus checkPostConditions(IProgressMonitor monitor) throws CoreException {
		return new RefactoringStatus();
	}
}
