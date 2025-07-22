/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation;
import org.eclipse.jdt.internal.corext.codemanipulation.AddDelegateMethodsOperation.DelegateEntry;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.corext.refactoring.structure.ASTNodeSearchUtil;
import org.eclipse.jdt.internal.corext.refactoring.util.RefactoringASTParser;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;

import org.eclipse.jdt.ui.tests.core.rules.Java16ProjectTestSetup;

/**
 * Tests generation of delegate methods
 */
public class GenerateDelegateMethodsTest16 extends SourceTestCase16 {
	@Rule
	public Java16ProjectTestSetup pts= new Java16ProjectTestSetup(false);

	@Before
	public void before() {
		String str= """
				/* (non-Javadoc)
				 * ${see_to_target}
				 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.DELEGATECOMMENT_ID, str, null);
	}


	public void runOperation(IType type, IField[] fields, String[] methodNames, IJavaElement insertBefore, boolean createComments) throws CoreException {

		assertEquals(fields.length, methodNames.length);

		RefactoringASTParser parser= new RefactoringASTParser(IASTSharedValues.SHARED_AST_LEVEL);
		CompilationUnit unit= parser.parse(type.getCompilationUnit(), true);


		DelegateEntry[] entries= new DelegateEntry[fields.length];

		for (int i= 0; i < fields.length; i++) {

			IField field= fields[i];
			// Fields
			VariableDeclarationFragment frag= ASTNodeSearchUtil.getFieldDeclarationFragmentNode(field, unit);
			IVariableBinding b= frag.resolveBinding();
			ITypeBinding typeBinding= b.getType();
			IMethodBinding[] methodBindings= typeBinding.getDeclaredMethods();
			IMethodBinding methodBinding= null;

			for (int j= 0; j < methodBindings.length; ++j) {
				if (methodBindings[j].getName().equals(methodNames[i])) {
					methodBinding= methodBindings[j];
					break;
				}
			}

			entries[i]= new DelegateEntry(methodBinding, b);
		}

		fSettings.createComments= createComments;

		AddDelegateMethodsOperation op= new AddDelegateMethodsOperation(unit, entries, insertBefore, fSettings, true, true);

		op.run(new NullProgressMonitor());
		JavaModelUtil.reconcile(type.getCompilationUnit());
	}

	// ------------- Actual tests


	/**
	 * Test delegate generation for record component methods
	 */
	@Test
	public void test13() throws Exception { //https://github.com/eclipse-jdt/eclipse.jdt.ui/issues/2362

		ICompilationUnit a= fPackageP.createCompilationUnit("A.java", "package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	private Y y;\r\n" +
				"\r\n" +
				"	public record Y(String a, Integer b) {\r\n" +
				"		public Integer b() {\r\n" +
				"			return b;\r\n" +
				"		}\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", true, null);

		IType type= a.getType("A");
		type.resolveType("Y");
		IField someField= a.getType("A").getField("y");

		runOperation(type, new IField[] { someField, someField }, new String[] { "a", "b" }, null, true);

		compareSource("package p;\r\n" +
				"\r\n" +
				"public class A {\r\n" +
				"	private Y y;\r\n" +
				"\r\n" +
				"	public record Y(String a, Integer b) {\r\n" +
				"		public Integer b() {\r\n" +
				"			return b;\r\n" +
				"		}\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.A.Y#a()\r\n" +
				"	 */\r\n" +
				"	public String a() {\r\n" +
				"		return y.a();\r\n" +
				"	}\r\n" +
				"\r\n" +
				"	/* (non-Javadoc)\r\n" +
				"	 * @see p.A.Y#b()\r\n" +
				"	 */\r\n" +
				"	public Integer b() {\r\n" +
				"		return y.b();\r\n" +
				"	}\r\n" +
				"}\r\n" +
				"", a.getSource());
	}

}
