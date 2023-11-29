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
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [introduce indirection] ClassCastException when introducing indirection on method in generic class - https://bugs.eclipse.org/395231
 *     Nikolay Metchev <nikolaymetchev@gmail.com> - [introduce indirection] Adds unneccessary import when inner class is used as parameter in surrounding class - https://bugs.eclipse.org/395228
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.code.IntroduceIndirectionRefactoring;

import org.eclipse.jdt.ui.tests.refactoring.infra.TextRangeUtil;
import org.eclipse.jdt.ui.tests.refactoring.rules.RefactoringTestSetup;

public class IntroduceIndirectionTests extends GenericRefactoringTest {
	private static final String REFACTORING_PATH= "IntroduceIndirection/";

	public IntroduceIndirectionTests() {
		rts= new RefactoringTestSetup();
	}

	protected IntroduceIndirectionTests(RefactoringTestSetup rts) {
		super(rts);
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	/**
	 *
	 * Invokes the introduce indirection ref. Some pointers:
	 *
	 * @param topLevelName This is an array of fully qualified top level(!) type names with exactly one package prefix (e.g. "p.Foo").
	 * 					   Simple names must correspond to .java files.
	 * 					   The first cu will be used for the invocation of the refactoring (see positioning)
	 * @param newName name of indirection method
	 * @param qTypeName qualified type name of the type for the indirection method. Should be one of the cus in topLevelName.
	 * @param startLine starting line of selection in topLevelName[0]
	 * @param startColumn starting column of selection in topLevelName[0]
	 * @param endLine ending line of selection in topLevelName[0]
	 * @param endColumn ending column of selection in topLevelName[0]
	 * @param updateReferences true if references should be updated
	 * @param shouldWarn if true, warnings will be expected in the result
	 * @param shouldError if true, errors will be expected in the result
	 * @param shouldFail if true, fatal errors will be expected in the result
	 */
	private void helper(String[] topLevelName, String newName, String qTypeName, int startLine, int startColumn, int endLine, int endColumn, boolean updateReferences, boolean shouldWarn,
			boolean shouldError, boolean shouldFail) throws Exception, JavaModelException, CoreException, IOException {
		ICompilationUnit[] cu= new ICompilationUnit[topLevelName.length];
		for (int i= 0; i < topLevelName.length; i++) {
			String packName= topLevelName[i].substring(0, topLevelName[i].indexOf('.'));
			String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
			IPackageFragment cPackage= getRoot().createPackageFragment(packName, true, null);
			cu[i]= createCUfromTestFile(cPackage, className);
		}

		ISourceRange selection= TextRangeUtil.getSelection(cu[0], startLine, startColumn, endLine, endColumn);
		try {
			IntroduceIndirectionRefactoring ref= new IntroduceIndirectionRefactoring(cu[0], selection.getOffset(), selection.getLength());
			ref.setEnableUpdateReferences(updateReferences);
			if (qTypeName != null)
				ref.setIntermediaryTypeName(qTypeName);
			if (newName != null)
				ref.setIntermediaryMethodName(newName);

			boolean failed= false;
			RefactoringStatus status= performRefactoringWithStatus(ref);
			if (status.hasFatalError()) {
				assertTrue("Failed but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.FATAL), shouldFail);
				failed= true;
			} else
				assertFalse("Didn't fail although expected", shouldFail);

			if (!failed) {

				if (status.hasError())
					assertTrue("Had errors but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.ERROR), shouldError);
				else
					assertFalse("No error although expected", shouldError);

				if (status.hasWarning())
					assertTrue("Had warnings but shouldn't: " + status.getMessageMatchingSeverity(RefactoringStatus.WARNING), shouldWarn);
				else
					assertFalse("No warning although expected", shouldWarn);

				for (int i= 0; i < topLevelName.length; i++) {
					String className= topLevelName[i].substring(topLevelName[i].indexOf('.') + 1);
					assertEqualLines("invalid output.", getFileContents(getOutputTestFileName(className)), cu[i].getSource());
				}
			}
		} finally {
			for (int i= 0; i < topLevelName.length; i++)
				JavaProjectHelper.delete(cu[i]);
		}
	}

	protected void helperPass(String[] topLevelName, String newName, String target, int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		helper(topLevelName, newName, target, startLine, startColumn, endLine, endColumn, true, false, false, false);
	}

	protected void helperWarn(String[] topLevelName, String newName, String target, int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		helper(topLevelName, newName, target, startLine, startColumn, endLine, endColumn, true, true, false, false);
	}

	protected void helperErr(String[] topLevelName, String newName, String target, int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		helper(topLevelName, newName, target, startLine, startColumn, endLine, endColumn, true, true, true, false);
	}

	protected void helperFail(String[] topLevelName, String newName, String target, int startLine, int startColumn, int endLine, int endColumn) throws Exception {
		helper(topLevelName, newName, target, startLine, startColumn, endLine, endColumn, true, true, true, true);
	}

	@Test
	public void test01() throws Exception {
		// very simple test
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 7, 10, 7, 13);
	}

	@Test
	public void test02() throws Exception {
		// warning if a super call is found
		helperWarn(new String[] { "p.A", "p.B" }, "bar", "p.A", 8, 15, 8, 18);
	}

	@Test
	public void test03() throws Exception {
		// add imports to target
		helperPass(new String[] { "p.Foo", "p.Bar" }, "bar", "p.Bar", 8, 17, 8, 20);
	}

	@Test
	public void test04() throws Exception {
		// this qualification with outer type, method declaration is in outer type.
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 10, 17, 10, 20);
	}

	@Test
	public void test05() throws Exception {
		// this qualification with outer type, method declaration is in
		// super type of outer type
		helperPass(new String[] { "p.Foo", "p.Bar" }, "bar", "p.Foo", 12, 17, 12, 27);
	}

	@Test
	public void test06() throws Exception {
		// this qualification with the current type, method declaration is
		// in super type of current type
		helperPass(new String[] { "p.Foo", "p.Bar" }, "bar", "p.Foo", 10, 13, 10, 23);
	}

	@Test
	public void test07() throws Exception {
		// test qualification with anonymous type (=> warning, don't update)
		helperWarn(new String[] { "p.E1" }, "bar", "p.E1", 30, 16, 30, 19);
	}

	@Test
	public void test08() throws Exception {
		// open hierarchy failure
		helperFail(new String[] { "p.SeaLevel", "p.Eiger", "p.Moench" }, "bar", "p.SeaLevel", 13, 11, 13, 14);
	}

	@Test
	public void test09() throws Exception {
		// create static intermediary
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 7, 17, 7, 20);
	}

	@Test
	public void test10() throws Exception {
		// error, method already exists
		helperErr(new String[] { "p.Foo", "p.Bar" }, "foo", "p.Foo", 10, 19, 10, 22);
	}

	@Test
	public void test11() throws Exception {
		// test name clash with existing argument
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 12, 9, 12, 12);
	}

	@Test
	public void test12() throws Exception {
		// cannot put the intermediary into an inner non-static type
		helperFail(new String[] { "p.Foo" }, "bar", "p.Foo.Inner", 9, 10, 9, 13);
	}

	@Test
	public void test13() throws Exception {
		// create intermediary inside nested static types
		helperPass(new String[] { "p.Foo", "p.Bar" }, "bar", "p.Foo.Inner.MoreInner", 13, 10, 13, 13);
	}

	@Test
	public void test14() throws Exception {
		// raise visibility of target so intermediary sees it.
		helperWarn(new String[] { "p0.Foo", "p1.Bar" }, "bar", "p1.Bar", 8, 18, 8, 23);
	}

	@Test
	public void test15() throws Exception {
		// raise visibility of intermediary type so
		// existing references see it
		helperWarn(new String[] { "p0.Foo", "p0.Bar", "p1.Third" }, "bar", "p0.Bar", 8, 17, 8, 20);
	}

	@Test
	public void test16() throws Exception {
		// test non-reference mode with a method invocation selected
		helper(new String[] { "p.Bar", "p.Foo" }, "bar", "p.Bar", 6, 19, 6, 22, false, false, false, false);
	}

	@Test
	public void test17() throws Exception {
		// generic target method
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 16, 9, 16, 12);
	}

	@Test
	public void test18() throws Exception {
		// simple test with generic type, unused
		try {
			getRoot().getJavaProject().setOption(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_METHOD_TYPE_PARAMETERS, JavaCore.ENABLED);
			helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 19, 11, 19, 14);
		} finally {
			getRoot().getJavaProject().setOption(JavaCore.COMPILER_PB_MISSING_JAVADOC_TAGS_METHOD_TYPE_PARAMETERS, JavaCore.DISABLED);
		}

	}

	@Test
	public void test19() throws Exception {
		// simple test with generic type, used
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 19, 11, 19, 14);
	}

	@Test
	public void test20() throws Exception {
		// complex case with generic type parameters and method parameters used
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 11, 11, 11, 17);
	}

	@Test
	public void test21() throws Exception {
		// no call updating if type arguments are used
		helperWarn(new String[] { "p.Foo" }, "bar", "p.Foo", 9, 22, 9, 26);
	}

	@Test
	public void test22() throws Exception {
		// method using type parameters from enclosing types as well
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 16, 24, 16, 27);
	}

	@Test
	public void test23() throws Exception {
		// warn about incorrect qualified static calls and don't update them.
		helperWarn(new String[] { "p.Foo" }, "bar", "p.Foo", 11, 25, 11, 28);
	}

	@Test
	public void test24() throws Exception {
		// assure common super type is used even if the hierarchy branches downwards
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 11, 11, 11, 14);
	}

	@Test
	public void test25() throws Exception {
		// increase visibility of overridden methods as well
		helperWarn(new String[] { "p0.Foo", "p0.SubFoo", "p1.Bar" }, "bar", "p1.Bar", 8, 20, 8, 23);
	}

	@Test
	public void test26() throws Exception {
		// ensure exceptions are copied
		helperPass(new String[] { "p.Foo" }, "bar", "p.Foo", 7, 24, 7, 27);
	}

	@Test
	public void test27() throws Exception {
		// complex visibility adjustment case
		// target method is not inside target type, and is overridden
		// target type must be increased, and all overridden methods must be increased.
		helperWarn(new String[] { "p0.Foo", "p0.RealFoo", "p0.NonOriginalSubFoo", "p0.VerySuperFoo", "p1.Bar" }, "bar", "p1.Bar", 7, 13, 7, 16);
	}

	@Test
	public void test28() throws Exception {
		// the template for the intermediary must be the method inside the real
		// target (for parameter names and exceptions)
		helperWarn(new String[] { "p.Foo", "p.Bar",}, "bar", "p.Foo", 10, 9, 10, 12);
	}

	@Test
	public void test29() throws Exception {
		// don't adjust visibility twice
		helperWarn(new String[] { "p0.Test", "p1.Other" }, "bar", "p1.Other", 5, 35, 5, 44);
	}

	@Test
	public void test30() throws Exception {
		// multiple generic instantiations
		helperPass(new String[] { "p.MultiGenerics" }, "bar", "p.MultiGenerics", 7, 16, 7, 26);
	}

	@Test
	public void test31() throws Exception {
		// test for bug 127665
		helperPass(new String[] { "p.Test" }, "foo", "p.Test0", 13, 20, 13, 23);
	}

	@Test
	public void test32() throws Exception {
		// test for bug 395231
		helperPass(new String[] { "p.Test" }, "foo", "p.Test0", 4, 10, 4, 13);
	}

	@Test
	public void test33() throws Exception {
		// test for bug 395231
		helper(new String[] { "p.Test" }, "foo", "p.Test0", 8, 9, 8, 20, false, false, false, false);
	}

	@Test
	public void test34() throws Exception {
		// test for bug 395231
		helperPass(new String[] { "p.Test" }, "foo", "p.Test0", 6, 9, 6, 13);
	}

	@Test
	public void test35() throws Exception {
		// test for bug 395231
		helperPass(new String[] { "p.Test" }, "foo", "p.Test0", 6, 9, 6, 13);
	}

	@Test
	public void test36() throws Exception {
		// test for bug 395231
		helperPass(new String[] { "p.Test" }, "foo", "p.Test0", 6, 9, 6, 13);
	}

	@Test
	public void test37() throws Exception {
		// test for bug 127665
		helperPass(new String[] { "p.Test" }, "foo", "p.Test0", 4, 10, 4, 13);
	}

	@Test
	public void test38() throws Exception {
		// test for bug 127665
		helperPass(new String[] { "p.Test" }, "foo2", "p.Test0", 5, 21, 5, 24);
	}

	@Test
	public void test39() throws Exception {
		// test bug 127984
		helperWarn(new String[] { "p.Foo", "p.Bar" }, "foo", "p.Bar", 5, 17, 5, 20);
	}

	@Test
	public void test40() throws Exception {
		// test bug 127984
		helperPass(new String[] { "p.Foo", "p.Bar" }, "foo", "p.Bar", 5, 20, 5, 23);
	}

	@Test
	public void test41() throws Exception {
		// test bug 127984
		helperPass(new String[] { "p.Foo" }, "foo", "p.Foo.Bar", 5, 18, 5, 20);
	}

	@Test
	public void test42() throws Exception {
		// test bug 127984
		helperPass(new String[] { "p.Foo" }, "foo", "p.Foo", 9, 27, 9, 29);
	}

	@Test
	public void test43() throws Exception {
		// test bug 127984
		helperWarn(new String[] { "p.Foo", "q.Bar" }, "foo", "q.Bar", 5, 20, 5, 23);
	}

	@Test
	public void test44() throws Exception {
		// test bug 127984
		helperWarn(new String[] { "p.Foo", "p.Bar" }, "foo", "p.Bar", 5, 18, 5, 21);
	}
}
