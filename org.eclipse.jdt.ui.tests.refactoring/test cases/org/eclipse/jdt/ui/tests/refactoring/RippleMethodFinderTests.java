/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder2;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractJunit4CUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class RippleMethodFinderTests extends AbstractJunit4CUTestCase {

	private static final String REFACTORING_PATH= "RippleMethodFinder/";
	private static final String TARGET= "/*target*/";
	private static final String RIPPLE= "/*ripple*/";

	@Rule
	public RefactoringTestSetup rts= new RefactoringTestSetup();

	@Override
	protected String getResourceLocation() {
		return REFACTORING_PATH;
	}

	@Override
	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	private void perform() throws Exception {
		IPackageFragment pack= rts.getPackageP();
		String name= adaptName("A_" + getName());
		ICompilationUnit cu= createCU(pack, name, getFileInputStream(getResourceLocation() + "/" + name));

		String contents= cu.getBuffer().getContents();

		IJavaElement[] elements= cu.codeSelect(contents.indexOf(TARGET) + TARGET.length(), 0);
		assertEquals(1, elements.length);
		IMethod target= (IMethod) elements[0];

		List<IMethod> rippleMethods= new ArrayList<>();
		rippleMethods.add(target);
		int start= 0;
		while (start < contents.length()) {
			start= contents.indexOf(RIPPLE, start);
			if (start == -1)
				break;
			elements= cu.codeSelect(start + RIPPLE.length(), 0);
			assertEquals(1, elements.length);
			IMethod rippleMethod= (IMethod) elements[0];
			rippleMethods.add(rippleMethod);
			start++;
		}

		for (IMethod method : RippleMethodFinder2.getRelatedMethods(target, new NullProgressMonitor(), null)) {
			assertTrue("method not found: " + method, rippleMethods.remove(method));
		}
		assertEquals("found wrong ripple methods: " + rippleMethods, 0, rippleMethods.size());
	}

	@Test
	public void test1() throws Exception {
		perform();
	}
	@Test
	public void test2() throws Exception {
		perform();
	}
	@Test
	public void test3() throws Exception {
		perform();
	}
	@Test
	public void test4() throws Exception {
		perform();
	}
	@Test
	public void test5() throws Exception {
		perform();
	}
	@Test
	public void test6() throws Exception {
		perform();
	}
	@Test
	public void test7() throws Exception {
		perform();
	}
	@Test
	public void test8() throws Exception {
		perform();
	}
	@Test
	public void test9() throws Exception {
		perform();
	}
	@Test
	public void test10() throws Exception {
		perform();
	}
	@Test
	public void test11() throws Exception {
		perform();
	}
	@Test
	public void test12() throws Exception {
		perform();
	}

}
