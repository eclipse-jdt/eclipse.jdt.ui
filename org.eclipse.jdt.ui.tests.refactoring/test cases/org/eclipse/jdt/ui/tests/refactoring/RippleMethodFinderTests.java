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
package org.eclipse.jdt.ui.tests.refactoring;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.refactoring.rename.RippleMethodFinder2;

import org.eclipse.jdt.ui.tests.refactoring.infra.AbstractCUTestCase;
import org.eclipse.jdt.ui.tests.refactoring.infra.RefactoringTestPlugin;


public class RippleMethodFinderTests extends AbstractCUTestCase {

	private final static boolean BUG_96761_core_finds_non_overriding= true;

	private static final Class clazz= RippleMethodFinderTests.class;
	private static final String REFACTORING_PATH= "RippleMethodFinder/";
	private static final String TARGET= "/*target*/";
	private static final String RIPPLE= "/*ripple*/";

	public RippleMethodFinderTests(String name) {
		super(name);
	}

	protected String getResourceLocation() {
		return REFACTORING_PATH;
	}

	public static Test suite() {
		return new RefactoringTestSetup(new TestSuite(clazz));
	}

	public static Test setUpTest(Test someTest) {
		return new RefactoringTestSetup(someTest);
	}

	protected InputStream getFileInputStream(String fileName) throws IOException {
		return RefactoringTestPlugin.getDefault().getTestResourceStream(fileName);
	}

	private void perform() throws Exception {
		ICompilationUnit cu= null;
		try {
			IPackageFragment pack= RefactoringTestSetup.getPackageP();
			String name= adaptName("A_" + getName());
			cu= createCU(pack, name, getFileInputStream(getResourceLocation() + "/" + name));

			String contents= cu.getBuffer().getContents();

			IJavaElement[] elements= cu.codeSelect(contents.indexOf(TARGET) + TARGET.length(), 0);
			assertEquals(1, elements.length);
			IMethod target= (IMethod) elements[0];

			List/*<IMethod>*/ rippleMethods= new ArrayList();
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

			IMethod[] result= RippleMethodFinder2.getRelatedMethods(target, new NullProgressMonitor(), null);
			for (int i= 0; i < result.length; i++) {
				IMethod method= result[i];
				assertTrue("method not found: " + method, rippleMethods.remove(method));
			}
			assertEquals("found wrong ripple methods: " + rippleMethods, 0, rippleMethods.size());
		} finally {
			if (cu != null)
				cu.delete(true, null);
		}
	}

	public void test1() throws Exception {
		perform();
	}
	public void test2() throws Exception {
		perform();
	}
	public void test3() throws Exception {
		perform();
	}
	public void test4() throws Exception {
		perform();
	}
	public void test5() throws Exception {
		perform();
	}
	public void test6() throws Exception {
		perform();
	}
	public void test7() throws Exception {
		perform();
	}
	public void test8() throws Exception {
		perform();
	}
	public void test9() throws Exception {
		perform();
	}
	public void test10() throws Exception {
		perform();
	}
	public void test11() throws Exception {
		if (BUG_96761_core_finds_non_overriding)
			return;
		perform();
	}
	public void test12() throws Exception {
		if (BUG_96761_core_finds_non_overriding)
			return;
		perform();
	}

}
