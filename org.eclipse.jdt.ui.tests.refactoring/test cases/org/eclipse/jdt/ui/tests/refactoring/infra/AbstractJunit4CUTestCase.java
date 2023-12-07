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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.rules.TestName;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.tests.refactoring.GenericRefactoringTest;

public abstract class AbstractJunit4CUTestCase {

	@Rule
	public TestName tn= new TestName();

	protected String getName() {
		return tn.getMethodName();
	}

	protected String getFileContents(InputStream in) throws IOException {
		return new String(in.readAllBytes());
	}

	protected ICompilationUnit createCU(IPackageFragment pack, String name, String contents) throws Exception {
		ICompilationUnit cu= pack.createCompilationUnit(name, contents, true, null);
		cu.save(null, true);
		return cu;
	}

	protected ICompilationUnit createCU(IPackageFragment pack, String name, InputStream contents) throws Exception {
		return createCU(pack, name, getFileContents(contents));
	}

	//--- creating a compilation unit from a resource folder relative to a plugin ----------------------------------

	protected abstract InputStream getFileInputStream(String fileName) throws IOException;

	protected String getResourceLocation() {
		return "";
	}

	protected ICompilationUnit createCU(IPackageFragment pack, String name) throws Exception {
		name= adaptName(name);
		try (InputStream fileInputStream= getFileInputStream(getFilePath(pack, name))) {
			return createCU(pack, name, fileInputStream);
		}
	}

	protected String adaptName(String name) {
		return name + ".java";
	}

	protected String getProofedContent(String folder, String name) throws Exception {
		name= adaptName(name);
		try (InputStream fileInputStream= getFileInputStream(getFilePath(folder, name))) {
			return getFileContents(fileInputStream);
		}
	}

	private String getFilePath(String path, String name) {
		return getResourceLocation() + path + "/" + name;
	}

	protected String getFilePath(IPackageFragment pack, String name) {
		return getFilePath(pack.getElementName(), name);
	}

	//---- helper to compare two file without considering the package statement

	public static void compareSource(String refactored, String proofed) {
		compareSource(refactored, proofed, true);
	}

	public static void compareSource(String refactored, String proofed, boolean skipPackageDeclaration) {
		int index= skipPackageDeclaration ? refactored.indexOf(';'): 0;
		refactored= refactored.substring(index);
		index= skipPackageDeclaration ? proofed.indexOf(';') : 0;
		proofed= proofed.substring(index);
		GenericRefactoringTest.assertEqualLines(proofed, refactored);
	}
}
