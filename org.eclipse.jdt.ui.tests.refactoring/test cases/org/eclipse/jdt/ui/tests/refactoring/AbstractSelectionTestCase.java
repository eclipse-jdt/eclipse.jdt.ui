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
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractCUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;

import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public abstract class AbstractSelectionTestCase extends AbstractCUTestCase {

	public static final String SQUARE_BRACKET_OPEN= "/*[*/";
	public static final int    SQUARE_BRACKET_OPEN_LENGTH= SQUARE_BRACKET_OPEN.length();
	public static final String SQUARE_BRACKET_CLOSE=   "/*]*/";
	public static final int    SQUARE_BRACKET_CLOSE_LENGTH= SQUARE_BRACKET_CLOSE.length();
	
	protected static final int VALID_SELECTION=     1;
	protected static final int INVALID_SELECTION=   2;
	protected static final int COMPARE_WITH_OUTPUT= 3;
	
	public AbstractSelectionTestCase(String name) {
		super(name);
	}

	protected int[] getSelection(String source) {
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
		
		assertTrue("Selection invalid", start >= 0 && end >= 0 && end >= start);
		
		int[] result= new int[] { start, end - start }; 
		// System.out.println("|"+ source.substring(result[0], result[0] + result[1]) + "|");
		return result;
	}
	
	protected ITextSelection getTextSelection(String source) {
		int[] s= getSelection(source);
		return new TextSelection(s[0], s[1]);
	}
	
	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}
	
	protected void performTest(final ICompilationUnit unit, final Refactoring refactoring, int mode, final String out, boolean doUndo) throws Exception {
		IProgressMonitor pm= new NullProgressMonitor();
		switch (mode) {
			case VALID_SELECTION:
				assertTrue(checkPreconditions(refactoring, pm).isOK());
				break;
			case INVALID_SELECTION:
				assertTrue(!checkPreconditions(refactoring, pm).isOK());
				break;
			case COMPARE_WITH_OUTPUT:
				IUndoManager undoManager= RefactoringCore.getUndoManager();
				undoManager.flush();
				String original= unit.getSource();
				
				PerformRefactoringOperation op= new PerformRefactoringOperation(
					refactoring, getCheckingStyle());
				JavaCore.run(op, new NullProgressMonitor());
				assertTrue("Precondition check failed", !op.getConditionStatus().hasFatalError());
				assertTrue("Validation check failed", !op.getValidationStatus().hasFatalError());
				assertNotNull("No Undo", op.getUndoChange());
				compareSource(out, unit.getSource());
				Change undo= op.getUndoChange();
				assertNotNull("Undo doesn't exist", undo);
				assertTrue("Undo manager is empty", undoManager.anythingToUndo());

				if (doUndo) {
					undoManager.performUndo(new NullProgressMonitor());
					assertTrue("Undo manager still has undo", !undoManager.anythingToUndo());
					assertTrue("Undo manager is empty", undoManager.anythingToRedo());
					compareSource(original, unit.getSource());
				}
				break;		
		}
	}
	
	protected RefactoringStatus checkPreconditions(Refactoring refactoring, IProgressMonitor pm) throws CoreException {
		CheckConditionsOperation op= new CheckConditionsOperation(refactoring, getCheckingStyle());
		op.run(new NullProgressMonitor());
		return op.getStatus();
	}
	
	protected int getCheckingStyle() {
		return CheckConditionsOperation.ALL_CONDITIONS;
	}
}
