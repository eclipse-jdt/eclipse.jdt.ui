/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import junit.framework.TestCase;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.ui.tests.refactoring.RefactoringTest;

public abstract class AbstractCUTestCase extends TestCase {

	public AbstractCUTestCase(String name) {
		super(name);
	}

	protected String getFileContents(InputStream in) throws IOException {
		BufferedReader br= new BufferedReader(new InputStreamReader(in));

		StringBuffer sb= new StringBuffer();
		try {
			int read= 0;
			while ((read= br.read()) != -1)
				sb.append((char) read);
		} finally {
			br.close();
		}
		return sb.toString();
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
		return createCU(pack, name, getFileInputStream(getFilePath(pack, name)));
	}

	protected String adaptName(String name) {
		return name + ".java";
	}

	protected String getProofedContent(String folder, String name) throws Exception {
		name= adaptName(name);
		return getFileContents(getFileInputStream(getFilePath(folder, name)));
	}

	private String getFilePath(String path, String name) {
		return getResourceLocation() + path + "/" + name;
	}

	private String getFilePath(IPackageFragment pack, String name) {
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
		RefactoringTest.assertEqualLines(proofed, refactored);
	}
}
