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
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IWorkspaceRunnable;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.corext.refactoring.base.ChangeContext;
import org.eclipse.jdt.internal.corext.refactoring.base.IChange;
import org.eclipse.jdt.internal.corext.refactoring.base.Refactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractCUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;
import org.eclipse.jdt.ui.tests.refactoring.infra.TestExceptionHandler;
import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;

/**
 * Test Naming Convention:<p>
 * - testSimple_A() -> package 'simple', file 'A.java'; result in package 'simple.out'<br>
 * - testSuch_ALongName17() -> package 'such', file 'ALongName17.java'
 */
public class LineColumnSelectionTestCase extends AbstractCUTestCase {

	public LineColumnSelectionTestCase(String name) {
		super(name);
	}

	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	/**
	 * @param name e.g. "testSuch_ALongName17"
	 * @return e.g. "ALongName17"
	 */
	protected String adaptName(String name) {
		int separator= name.indexOf('_');
		assertTrue(separator != -1);
		assertTrue(separator >= 5);
		return name.substring(separator + 1) + ".java";
	}

	/**
	 * @param name e.g. "testSuch_ALongName17"
	 * @return e.g. "such"
	 */
	protected String adaptPackage(String name) {
		int separator= name.indexOf('_');
		assertTrue(separator != -1);
		assertTrue(separator >= 5);
		return Character.toLowerCase(name.charAt(4))
				+ name.substring(5, separator);
	}

	/**
	 * get selection from comment in source "//selection: line, col, line, col"
	 * <br>relies on tabwidth == 4
	 */
	protected ISourceRange getSelection(ICompilationUnit cu) throws Exception {
		String source= cu.getSource();
		String selection= "//selection:";
		int selStart= source.indexOf(selection);
		assertTrue(selStart != -1);
	
		int dataStart= selStart + selection.length();
		StringTokenizer tokenizer= new StringTokenizer(source.substring(dataStart), " ,\t\r\n");
		int line1= Integer.parseInt(tokenizer.nextToken());
		int col1= Integer.parseInt(tokenizer.nextToken());
		int line2= Integer.parseInt(tokenizer.nextToken());
		int col2= Integer.parseInt(tokenizer.nextToken());
		return TextRangeUtil.getSelection(cu, line1, col1, line2, col2);
	}

	/** @require refactoring.checkActivation().isOK() */
	protected void performTest(ICompilationUnit unit, Refactoring refactoring, String out) throws Exception {
		IProgressMonitor pm= new NullProgressMonitor();
		String original= unit.getSource();
		final IChange change= refactoring.createChange(pm);
		assertNotNull(change);
		final ChangeContext context= new ChangeContext(new TestExceptionHandler());
		change.aboutToPerform(context, pm);
		JavaCore.run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				change.perform(context, monitor);
			}
		}, pm);
		change.performed();
		
		final IChange undo= change.getUndoChange();
		assertNotNull(undo);
		compareSource(unit.getSource(), out);
		final ChangeContext context2= new ChangeContext(new TestExceptionHandler());
		undo.aboutToPerform(context, pm);
		JavaCore.run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				undo.perform(context2, monitor);
			}
		}, pm);
		undo.performed();
		
		compareSource(unit.getSource(), original);
	}

}
