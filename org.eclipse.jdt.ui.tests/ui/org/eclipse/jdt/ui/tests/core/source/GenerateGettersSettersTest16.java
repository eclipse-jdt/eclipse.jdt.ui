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
package org.eclipse.jdt.ui.tests.core.source;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Modifier;

import org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.IRequestQuery;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

/**
 * Tests generation of getters and setters.
 *
 * @see org.eclipse.jdt.internal.corext.codemanipulation.AddGetterSetterOperation
 */
public class GenerateGettersSettersTest16 extends SourceTestCase16 {
	@Rule
	public ProjectTestSetup pts= new ProjectTestSetup();

	private static final IField[] NOFIELDS= new IField[] {};

	/**
	 * Create and run a new getter/setter operation; do not ask for anything
	 * (always skip setters for final fields; never overwrite existing methods).
	 * @param type the type
	 * @param getters fields to create getters for
	 * @param setters fields to create setters for
	 * @param gettersAndSetters fields to create getters and setters for
	 * @param sort enable sort
	 * @param visibility visibility for new methods
	 * @param sibling element to insert before
	 */
	private void runOperation(IType type, IField[] getters, IField[] setters, IField[] gettersAndSetters, boolean sort, int visibility, IJavaElement sibling) throws CoreException {

		IRequestQuery allYes= member -> IRequestQuery.YES_ALL;

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);

		AddGetterSetterOperation op= new AddGetterSetterOperation(type, getters, setters, gettersAndSetters, unit, allYes, sibling, fSettings, true, true);
		op.setSort(sort);
		op.setVisibility(visibility);

		op.run(new NullProgressMonitor());

		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	private void runOperation(IField[] getters, IField[] setters, IField[] gettersAndSetters) throws CoreException {
		runOperation(fRecordA, getters, setters, gettersAndSetters, false, Modifier.PUBLIC, null);
	}

	// --------------------- Actual tests

	/**
	 * No setter for final fields (if skipped by user, as per parameter)
	 */
	@Test
	public void testBug561413() throws Exception {

		runOperation(fRecordA.getRecordComponents(), NOFIELDS, NOFIELDS);

		String expected= """
			public record A(int x, String y) {\r
			\r
				/**\r
				 * @return Returns the x.\r
				 */\r
				public int x() {\r
					return x;\r
				}\r
			\r
				/**\r
				 * @return Returns the y.\r
				 */\r
				public String y() {\r
					return y;\r
				}\r
			}""";

		compareSource(expected, fRecordA.getSource());
	}
}
