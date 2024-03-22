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
package org.eclipse.jdt.ui.tests.core.source;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.testplugin.StringAsserts;
import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettings;

import org.eclipse.jdt.internal.ui.preferences.JavaPreferencesSettings;

public class SourceTestCase {

	@Rule
	public TestName tn=new TestName();

	private IJavaProject fJavaProject;

	protected IPackageFragment fPackageP;

	protected CodeGenerationSettings fSettings;

	protected IType fClassA;

	private ICompilationUnit fCuA;

	protected IPackageFragmentRoot fRoot;

	private void initCodeTemplates() {
		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "999");
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD, "1");
		options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_METHOD, "1");
		JavaCore.setOptions(options);

		String getterComment= """
			/**\r
			 * @return Returns the ${bare_field_name}.\r
			 */""";
		String getterBody= "return ${field};";

		String setterComment= """
			/**\r
			 * @param ${param} The ${bare_field_name} to set.\r
			 */""";
		String setterBody= "${field} = ${param};";

		StubUtility.setCodeTemplate(CodeTemplateContextType.GETTERCOMMENT_ID, getterComment, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.SETTERCOMMENT_ID, setterComment, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.GETTERSTUB_ID, getterBody, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.SETTERSTUB_ID, setterBody, null);


		String methodComment= """
			/**\r
			 * ${tags}\r
			 */""";
		String methodBody= "// ${todo} Auto-generated method stub\r\n" + "${body_statement}";

		String constructorComment= """
			/**\r
			 * ${tags}\r
			 */""";
		String constructorBody= "${body_statement}\r\n" + "// ${todo} Auto-generated constructor stub";

		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODCOMMENT_ID, methodComment, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.METHODSTUB_ID, methodBody, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, constructorComment, null);
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORSTUB_ID, constructorBody, null);

		fSettings= JavaPreferencesSettings.getCodeGenerationSettings((IJavaProject)null);
		fSettings.createComments= true;
	}

	@Before
	public void setUp() throws CoreException {

		fJavaProject= JavaProjectHelper.createJavaProject("DummyProject", "bin");
		assertNotNull(JavaProjectHelper.addRTJar(fJavaProject));

		fRoot= JavaProjectHelper.addSourceContainer(fJavaProject, "src");
		fPackageP= fRoot.createPackageFragment("p", true, null);
		fCuA= fPackageP.getCompilationUnit("A.java");
		fClassA= fCuA.createType("public class A {\n}\n", null, true, null);

		initCodeTemplates();
	}

	@After
	public void tearDown() throws Exception {
		JavaProjectHelper.delete(fJavaProject);
		fJavaProject= null;
		fPackageP= null;
	}

	protected void compareSource(String expected, String actual) throws IOException {
		StringAsserts.assertEqualStringIgnoreDelim(actual, expected);
	}

}
