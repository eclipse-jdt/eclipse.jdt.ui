/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import org.junit.Before;
import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.internal.corext.codemanipulation.AddJavaDocStubOperation;


public class AddJavaDocStubOperationTest extends SourceTestCase {

	@Before
	@Override
	public void setUp() throws CoreException {
		super.setUp();
		JavaProjectHelper.set17CompilerOptions(fJavaProject, false);
	}


	public void runOperation(IMember[] members) throws CoreException {
		AddJavaDocStubOperation op= new AddJavaDocStubOperation(members);
		op.run(new NullProgressMonitor());
	}


	@Test
	public void testAddJavaDocToSimpleRecord() throws Exception {

		ICompilationUnit cu= fPackageP.createCompilationUnit(
				"InnerApp4.java",
				"""
						package p;

						record InnerApp4<T>(int p) {}
						""",
				true,
				null);
		IType rec= cu.getTypes()[0];
		cu.becomeWorkingCopy(new NullProgressMonitor());
		try {
			runOperation(new IMember[] { rec });
			cu.commitWorkingCopy(true, new NullProgressMonitor());
		} finally {
			cu.discardWorkingCopy();
		}

		compareSource("""
				package p;

				/**
				 * @param <T>
				 * @param p
				 */
				record InnerApp4<T>(int p) {}
				""", cu.getSource());
	}

	@Test
	public void testAddJavaDocToSimpleRecord2() throws Exception {

		ICompilationUnit cu= fPackageP.createCompilationUnit(
				"InnerApp4.java",
				"""
						package p;

						record InnerApp4<T>(int p, String x) {}
						""",
				true,
				null);
		IType rec= cu.getTypes()[0];
		cu.becomeWorkingCopy(new NullProgressMonitor());
		try {
			runOperation(new IMember[] { rec });
			cu.commitWorkingCopy(true, new NullProgressMonitor());
		} finally {
			cu.discardWorkingCopy();
		}

		compareSource("""
				package p;

				/**
				 * @param <T>
				 * @param p
				 * @param x
				 */
				record InnerApp4<T>(int p, String x) {}
				""", cu.getSource());
	}

}
