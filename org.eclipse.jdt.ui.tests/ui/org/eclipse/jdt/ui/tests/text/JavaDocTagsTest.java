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
package org.eclipse.jdt.ui.tests.text;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;

import org.eclipse.jdt.ui.tests.core.CoreTests;
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

import org.eclipse.jdt.internal.corext.javadoc.JavaDocAccess;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocCommentReader;
import org.eclipse.jdt.internal.corext.javadoc.JavaDocTag;

/**
 *
 */
public class JavaDocTagsTest extends CoreTests {

	private static final Class THIS= JavaDocTagsTest.class;
	
	private IJavaProject fJProject1;
	private IPackageFragmentRoot fSourceFolder;

	public JavaDocTagsTest(String name) {
		super(name);
	}


	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}
	
	public static Test suite() {
		if (true) {
			return allTests();
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new JavaDocTagsTest("testMethodWithConstructorName"));
			return new ProjectTestSetup(suite);
		}
	}


	protected void setUp() throws Exception {
		fJProject1= ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
	}


	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, ProjectTestSetup.getDefaultClasspath());
	}
	
	
	public void testJavadocTags1() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * First sentence.\n");
		buf.append("     * @version 1.1\n");
		buf.append("     * @deprecated Use something else.\n");
		buf.append("     * @throws An exception {@link A} and more.\n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		IType type= cu.getType("E");
		IMethod method= type.getMethod("foo", new String[0]);
		
		JavaDocCommentReader reader= JavaDocAccess.getJavaDoc(method, false);
		JavaDocTag[] tags= JavaDocTag.createFromComment(reader);
		
		String cuSource= cu.getSource();
		assertNumberOf("proposals", tags.length, 4);
		assertTag(tags[0], null, "First sentence.", "First sentence.\n     * ", "First sentence.", cuSource);
		assertTag(tags[1], "version", "1.1", "@version 1.1\n     * ", "1.1", cuSource);
		assertTag(tags[2], "deprecated", "Use something else.", "@deprecated Use something else.\n     * ", "Use something else.", cuSource);
		assertTag(tags[3], "throws", "An exception {@link A} and more.", "@throws An exception {@link A} and more.\n     ", "An exception {@link A} and more.", cuSource);		
	}
	
	public void testJavadocTags2() throws Exception {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("public class E {\n");
		buf.append("    /**\n");
		buf.append("     * {@link A}\n");
		buf.append("     * @version{@link A}\n");
		buf.append("     */\n");
		buf.append("    public void foo() {\n");
		buf.append("    }\n");
		buf.append("}\n");
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", buf.toString(), false, null);
		
		IType type= cu.getType("E");
		IMethod method= type.getMethod("foo", new String[0]);
		
		JavaDocCommentReader reader= JavaDocAccess.getJavaDoc(method, false);
		JavaDocTag[] tags= JavaDocTag.createFromComment(reader);
		
		String cuSource= cu.getSource();
		assertNumberOf("proposals", tags.length, 2);
		assertTag(tags[0], null, "{@link A}", "{@link A}\n     * ", "{@link A}", cuSource);
		assertTag(tags[1], "version", "{@link A}", "@version{@link A}\n     ", "@link A}", cuSource);
	}
	
	
	private static void assertTag(JavaDocTag tag, String expName, String expContent, String tagRangeContent, String contentRangeContent, String cuContent) {
		
		assertEqualString(tag.getName(), expName);
		assertEqualString(tag.getContent(), expContent);
		
		int start= tag.getOffset();
		if (expName != null) {
			String fullTag= '@' + expName;
			
			assertEqualString(cuContent.substring(start, start + fullTag.length()), fullTag);
		}
		
		assertEqualString(cuContent.substring(start, start + tag.getLength()), tagRangeContent);
		
		assertEqualString(cuContent.substring(tag.getContentOffset(), tag.getContentOffset() + tag.getContentLength()), contentRangeContent);
		
	}
	
}
