/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractJunit4CUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;

/**
 * Selection in the file read by {@link #getFileContents(InputStream)} is marked with
 * /*]*&#47; and /*[*&#47; (excluding the marker comments) or
 * /*[*&#47; and /*]*&#47; (including comments).
 */
public abstract class AbstractJunit4SelectionTestCase extends AbstractJunit4CUTestCase {
	public static final String SQUARE_BRACKET_OPEN= "/*[*/";
	public static final int    SQUARE_BRACKET_OPEN_LENGTH= SQUARE_BRACKET_OPEN.length();
	public static final String SQUARE_BRACKET_CLOSE=   "/*]*/";
	public static final int    SQUARE_BRACKET_CLOSE_LENGTH= SQUARE_BRACKET_CLOSE.length();

	enum TestMode { VALID_SELECTION, INVALID_SELECTION, COMPARE_WITH_OUTPUT }

	private boolean fIgnoreSelectionMarker;
	private int[] fSelection;
	protected boolean fIsPreDeltaTest;

	public AbstractJunit4SelectionTestCase() {
		this(false);
	}

	public AbstractJunit4SelectionTestCase(boolean ignoreSelectionMarker) {
		fIgnoreSelectionMarker= ignoreSelectionMarker;
	}

	@Before
	public void setUp() throws Exception {
		fIsPreDeltaTest= false;
	}

	protected int[] getSelection() {
		return fSelection;
	}

	protected ITextSelection getTextSelection() {
		int[] s= getSelection();
		return new TextSelection(s[0], s[1]);
	}

	@Override
	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	@Override
	protected String getFileContents(InputStream in) throws IOException {
		String result= super.getFileContents(in);
		initializeSelection(result);
		if (fIgnoreSelectionMarker) {
			result= result.replaceAll("/\\*\\[\\*/", "");
			result= result.replaceAll("/\\*\\]\\*/", "");
		}
		return result;
	}

	protected void performTest(final ICompilationUnit unit, final Refactoring refactoring, TestMode mode, final String out, boolean doUndo) throws Exception {
		IProgressMonitor pm= new NullProgressMonitor();
		switch (mode) {
			case VALID_SELECTION:
				assertTrue(checkPreconditions(refactoring, pm).isOK());
				break;
			case INVALID_SELECTION:
				assertFalse(checkPreconditions(refactoring, pm).isOK());
				break;
			case COMPARE_WITH_OUTPUT:
				IUndoManager undoManager= RefactoringCore.getUndoManager();
				undoManager.flush();
				String original= unit.getSource();

				final PerformRefactoringOperation op= new PerformRefactoringOperation(
					refactoring, getCheckingStyle());
				if (fIsPreDeltaTest) {
					IWorkspace workspace= ResourcesPlugin.getWorkspace();
					IResourceChangeListener listener= event -> TestModelProvider.assertTrue(event.getDelta());
					try {
						clearPreDelta();
						workspace.checkpoint(false);
						workspace.addResourceChangeListener(listener);
						JavaCore.run(op, new NullProgressMonitor());
					} finally {
						workspace.removeResourceChangeListener(listener);
					}
				} else {
					JavaCore.run(op, new NullProgressMonitor());
				}
				assertFalse("Precondition check failed: " + op.getConditionStatus().toString(), op.getConditionStatus().hasFatalError());
				assertFalse("Validation check failed: " + op.getConditionStatus().toString(), op.getValidationStatus().hasFatalError());
				assertNotNull("No Undo", op.getUndoChange());
				compareSource(unit.getSource(), out);
				Change undo= op.getUndoChange();
				assertNotNull("Undo doesn't exist", undo);
				assertTrue("Undo manager is empty", undoManager.anythingToUndo());

				if (doUndo) {
					undoManager.performUndo(null, new NullProgressMonitor());
					assertFalse("Undo manager still has undo", undoManager.anythingToUndo());
					assertTrue("Undo manager is empty", undoManager.anythingToRedo());
					compareSource(original, unit.getSource());
				}
				break;
			default:
				break;
		}
	}

	protected RefactoringStatus checkPreconditions(Refactoring refactoring, IProgressMonitor pm) throws CoreException {
		CheckConditionsOperation op= new CheckConditionsOperation(refactoring, getCheckingStyle());
		op.run(pm);
		return op.getStatus();
	}

	protected int getCheckingStyle() {
		return CheckConditionsOperation.ALL_CONDITIONS;
	}

	protected void clearPreDelta() {
		TestModelProvider.clearDelta();
	}

	private void initializeSelection(String source) {
		int start= -1;
		int end= -1;
		int includingStart= source.indexOf(SQUARE_BRACKET_OPEN);
		int excludingStart= source.indexOf(SQUARE_BRACKET_CLOSE);
		int includingEnd= source.lastIndexOf(SQUARE_BRACKET_CLOSE);
		int excludingEnd= source.lastIndexOf(SQUARE_BRACKET_OPEN);

		if (includingStart > excludingStart && excludingStart != -1) {
			includingStart= -1;
		} else if (excludingStart > includingStart && includingStart != -1) {
			excludingStart= -1;
		}

		if (includingEnd < excludingEnd) {
			includingEnd= -1;
		} else if (excludingEnd < includingEnd) {
			excludingEnd= -1;
		}

		if (includingStart != -1) {
			start= includingStart;
		} else {
			start= excludingStart + SQUARE_BRACKET_CLOSE_LENGTH;
		}

		if (excludingEnd != -1) {
			end= excludingEnd;
		} else {
			end= includingEnd + SQUARE_BRACKET_CLOSE_LENGTH;
		}

		assertTrue("Selection invalid", start >= 0);
		assertTrue("Selection invalid", end >= 0);
		assertTrue("Selection invalid", end >= start);

		fSelection= new int[] {
			start - (fIgnoreSelectionMarker ? SQUARE_BRACKET_CLOSE_LENGTH : 0),
			end - start
		};
		// System.out.println("|"+ source.substring(result[0], result[0] + result[1]) + "|");
	}
}
