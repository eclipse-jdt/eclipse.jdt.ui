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
package org.eclipse.jdt.text.tests.contentassist;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;

public class JavadocCompletionTest extends AbstractCompletionTest {
	/*
	 * This test tests only <= 1.5 source level tags.
	 */

	private static final Class THIS= JavadocCompletionTest.class;
	private static final String METHOD=
			"	public int method(int param) {\n" +
			"		return 0;\n" +
			"	}\n";
	private static final String FIELD= "	public int fField\n";

	private static final String[] TYPE_BLOCK_TAGS= {"@see", "@since", "@deprecated", "@serial", "@author", "@version", "@param", };
	private static final String[] METHOD_BLOCK_TAGS= {"@see", "@since", "@deprecated", "@param", "@return", "@throws", "@exception", "@serialData", };
	private static final String[] FIELD_BLOCK_TAGS= {"@see", "@since", "@deprecated", "@serial", "@serialField", };
	private static final String[] TYPE_INLINE_TAGS= {"@docRoot", "@link", "@linkplain", "@value", "@code", "@literal", };
	private static final String[] METHOD_INLINE_TAGS= {"@docRoot", "@inheritDoc", "@link", "@linkplain", "@value", "@code", "@literal", };
	private static final String[] FIELD_INLINE_TAGS= {"@docRoot", "@link", "@linkplain", "@value", "@code", "@literal", };
	private static final String[] HTML_TAGS= {"b", "blockquote", "br", "code", "dd", "dl", "dt", "em", "hr", "h1", "h2", "h3", "h4", "h5", "h6", "i", "li", "nl", "ol", "p", "pre", "q", "td", "th", "tr", "tt", "ul",};

	public static Test allTests() {
		return new TestSuite(THIS, suiteName(THIS));
	}

	public static Test setUpTest(Test test) {
		return new CompletionTestSetup(test);
	}

	public static Test suite() {
		return new CompletionTestSetup(allTests());
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, false);
	}

	public void testSeeType() throws Exception {
		assertTypeJavadocProposal(" * @see List|", "List ", " * @see java.util.List|");
	}

	public void testSeeTypeImportsOn() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * @see List|", "List ", " * @see List|");
	}

	public void testSeeTypeJavaLang() throws Exception {
		assertTypeJavadocProposal(" * @see Str|", "String ", " * @see String|");
	}

	public void testSeeImportedQualifiedType() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * @see java.util.Lis|", "List ", " * @see java.util.List|");
	}

	public void testSeeImportedType() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * @see Lis|", "List ", " * @see List|");
	}

	public void testSeeImportedTypeImportsOn() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		assertTypeJavadocProposal(" * @see Lis|", "List ", " * @see List|");
	}

	public void testSeeTypeSameType() throws Exception {
		assertTypeJavadocProposal(" * @see Comple|", "Completion_testSeeTypeSameType", " * @see Completion_testSeeTypeSameType|");
	}

	public void testSeeTypeSameTypeImportsOn() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		assertTypeJavadocProposal(" * @see Comple|", "Completion_testSeeTypeSameTypeImportsOn", " * @see Completion_testSeeTypeSameTypeImportsOn|");
	}

//	public void testInformalTypeReference() throws Exception {
//		assertTypeJavadocProposal(" * Prefix <code>List|</code> postfix", " * Prefix <code>Li|</code> postfix", "List ");
//	}
//
//	public void testInformalTypeReferenceImportsOn() throws Exception {
//		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
//		assertTypeJavadocProposal(" * Prefix <code>List|</code> postfix", " * Prefix <code>Li|</code> postfix", "List ");
//	}
//
//	public void testInformalTypeReferenceSameType() throws Exception {
//		assertTypeJavadocProposal(" * Prefix <code>Completion|</code> postfix", " * Prefix <code>Completion|</code> postfix", "Completion ");
//	}
//
	public void testSeeMethod() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#siz|", "size(", " * @see java.util.List#size()|");
	}

	public void testSeeMethodWithoutImport() throws Exception {
		if (true) {
			System.out.println("JavadocCompletionTest.testSeeMethodWithoutImport() - no best-effort imports with Core completion");
			return;
		}
		assertTypeJavadocProposal(" * @see List#siz|", "size(", " * @see List#size()|");
	}

	public void testSeeMethodWithParam() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#ge|", "get(", " * @see java.util.List#get(int)|");
	}

	public void testSeeMethodWithTypeVariableParameter() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#ad|", "add(O", " * @see java.util.List#add(Object)|");
	}

	public void testSeeMethodLocal() throws Exception {
		addMembers(METHOD);
		assertTypeJavadocProposal(" * @see #me|", "met", " * @see #method(int)|");
	}

	public void testSeeConstant() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.Collections#|", "EMPTY_LI", " * @see java.util.Collections#EMPTY_LIST|");
	}

	public void testLinkType() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link List|", "List ", " * Prefix {@link java.util.List|");
	}
	public void testLinkTypeClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link List|}", "List ", " * Prefix {@link java.util.List|}");
	}
	public void testDirectLinkType() throws Exception {
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link java.util.List}|");
	}
	public void testDirectLinkTypeNoAutoClose() throws Exception {
		if (true) {
			System.out.println("not testing autoclosing behavior, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544");
			return;
		}
		getJDTUIPrefs().setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, false);
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link java.util.List|");
	}

	public void testDirectLinkTypeImportsOn() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List}|");
	}

	public void testDirectLinkTypeExistingImport() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List}|");
	}

	public void testDirectLinkImportsOnExistingImport() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List}|");
	}

	public void testDirectLinkImportsOnExistingImportQualifiedPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix java.util.List|", "{@link List}", " * Prefix {@link java.util.List}|");
	}

	public void testDirectLinkImportsOnExistingImportCamelCase() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		addImport("java.text.DateFormat");
		expectImport("java.text.DateFormat");
		assertTypeJavadocProposal(" * Prefix DF|", "{@link DateFormat}", " * Prefix {@link DateFormat}|");
	}

	public void testDirectLinkImportsOnExistingImportCamelCaseQualifiedPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		addImport("java.text.DateFormat");
		expectImport("java.text.DateFormat");
		assertTypeJavadocProposal(" * Prefix java.text.DF|", "{@link DateFormat}", " * Prefix {@link java.text.DateFormat}|");
	}

	public void testDirectLinkTypeNoAutoCloseImportsOn() throws Exception {
		if (true) {
			System.out.println("not testing autoclosing behavior, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544");
			return;
		}
		expectImport("java.util.List");
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		getJDTUIPrefs().setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, false);
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List|");
	}

	public void testLinkTypeJavaLang() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Str|", "String ", " * Prefix {@link String|");
	}
	public void testLinkTypeJavaLangClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Str|}", "String ", " * Prefix {@link String|}");
	}

	public void testLinkTypeSameType() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Comple|", "Completion_testLinkTypeSameType", " * Prefix {@link Completion_testLinkTypeSameType|");
	}
	public void testLinkTypeSameTypeClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Comple|}", "Completion_testLinkTypeSameTypeClosed", " * Prefix {@link Completion_testLinkTypeSameTypeClosed|}");
	}

	public void testLinkMethodWithoutImport() throws Exception {
		if (true) {
			System.out.println("JavadocCompletionTest.testLinkMethodWithoutImport() - no best-effort imports with Core completion");
			return;
		}
		assertTypeJavadocProposal(" * Prefix {@link List#siz|", "size(", " * Prefix {@link List#size()|");
	}

	public void testLinkMethod() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#siz|", "size(", " * Prefix {@link java.util.List#size()|");
	}

	public void testLinkMethodLocal() throws Exception {
		addMembers(METHOD);
		assertTypeJavadocProposal(" * {@link #me|", "met", " * {@link #method(int)|");
	}

	public void testLinkMethodClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#siz|}", "size(", " * Prefix {@link java.util.List#size()|}");
	}

	public void testLinkMethodWithParam() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ge|", "get(", " * Prefix {@link java.util.List#get(int)|");
	}

	public void testLinkMethodWithParamNoOverwrite() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add} postfix", "clear", " * Prefix {@link java.util.List#clear()|add} postfix");
	}

	public void testLinkMethodWithParamNoOverwriteWithParams() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add(int, Object)} postfix", "clear", " * Prefix {@link java.util.List#clear()|add(int, Object)} postfix");
	}

	public void testLinkMethodWithParamOverwriteNoPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add} postfix", "clear", " * Prefix {@link java.util.List#clear()|} postfix");
	}

	public void testLinkMethodWithParamOverwrite() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#g|et} postfix", "get(", " * Prefix {@link java.util.List#get(int)|} postfix");
	}

	public void testLinkMethodWithParamOverwriteWithParamsNoPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add(int, Object)} postfix", "clear", " * Prefix {@link java.util.List#clear()|} postfix");
	}

	public void testLinkMethodWithParamOverwriteWithParams() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#g|et(long)} postfix", "get(", " * Prefix {@link java.util.List#get(int)|} postfix");
	}

	public void testLinkMethodWithParamClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ge|}", "get(", " * Prefix {@link java.util.List#get(int)|}");
	}

	public void testLinkMethodWithTypeVariableParameter() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ad|", "add(O", " * Prefix {@link java.util.List#add(Object)|");
	}
	public void testLinkMethodWithTypeVariableParameterClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ad|}", "add(O", " * Prefix {@link java.util.List#add(Object)|}");
	}

	public void testLinkConstant() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.Collections#|", "EMPTY_LI", " * Prefix {@link java.util.Collections#EMPTY_LIST|");
	}

	public void testFieldReference() throws Exception {
		assertTypeJavadocProposal(" * Prefix java.util.Collections#|", "{@link java.util.Collections#EMPTY_LI", " * Prefix {@link java.util.Collections#EMPTY_LIST}|");
	}

	public void testFieldValueReference() throws Exception {
		assertTypeJavadocProposal(" * Prefix java.util.Collections#|", "{@value java.util.Collections#EMPTY_LI", " * Prefix {@value java.util.Collections#EMPTY_LIST}|");
	}

	public void testMethodReference() throws Exception {
		assertTypeJavadocProposal(" * Prefix java.util.Collections#|", "{@link java.util.Collections#toSt", " * Prefix {@link java.util.Collections#toString()}|");
	}

	public void testTypeBlockTags() throws Exception {
		tearDown();
		for (int i= 0; i < TYPE_BLOCK_TAGS.length; i++) {
			setUp();
			String tag= TYPE_BLOCK_TAGS[i];
			assertTypeJavadocProposal(" * @|", tag, " * " + tag);
			tearDown();
		}
		setUp();
	}

	public void testMethodBlockTags() throws Exception {
		tearDown();
		for (int i= 0; i < METHOD_BLOCK_TAGS.length; i++) {
			setUp();
			addMembers(METHOD);
			String tag= METHOD_BLOCK_TAGS[i];
			assertMemberJavadocProposal(" * @|", tag, " * " + tag);
			tearDown();
		}
		setUp();
	}

	public void testFieldBlockTags() throws Exception {
		tearDown();
		for (int i= 0; i < FIELD_BLOCK_TAGS.length; i++) {
			setUp();
			addMembers(FIELD);
			String tag= FIELD_BLOCK_TAGS[i];
			assertMemberJavadocProposal(" * @|", tag, " * " + tag);
			tearDown();
		}
		setUp();
	}

 	public void testNoInlineAsBlockTags() throws Exception {
 		tearDown();
 		for (int i= 0; i < TYPE_INLINE_TAGS.length; i++) {
 			setUp();
			String tag= TYPE_INLINE_TAGS[i];
			assertNoMethodBodyProposals(" * @|", tag);
			tearDown();
		}
		setUp();
	}

	public void testTypeInlineTags() throws Exception {
 		tearDown();
		for (int i= 0; i < TYPE_INLINE_TAGS.length; i++) {
			setUp();
			String tag= TYPE_INLINE_TAGS[i];
			assertTypeJavadocProposal(" * {@|", "{" + tag + "}", " * {" + tag + "|}");
			tearDown();
		}
		setUp();
	}

	public void testMethodInlineTags() throws Exception {
		tearDown();
		for (int i= 0; i < METHOD_INLINE_TAGS.length; i++) {
			setUp();
			addMembers(METHOD);
			String tag= METHOD_INLINE_TAGS[i];
			assertMemberJavadocProposal(" * {@|", "{" + tag + "}", " * {" + tag + "|}");
			tearDown();
		}
		setUp();
	}

	public void testFieldInlineTags() throws Exception {
		tearDown();
		for (int i= 0; i < FIELD_INLINE_TAGS.length; i++) {
			setUp();
			addMembers(FIELD);
			String tag= FIELD_INLINE_TAGS[i];
			assertMemberJavadocProposal(" * {@|", "{" + tag + "}", " * {" + tag + "|}");
			tearDown();
		}
		setUp();
	}

	public void testNoBlockAsInlineTags() throws Exception {
		tearDown();
		for (int i= 0; i < TYPE_BLOCK_TAGS.length; i++) {
			String tag= TYPE_BLOCK_TAGS[i];
			setUp();
			assertNoMethodBodyProposals(" * {@|", tag);
			tearDown();
		}
		setUp();
	}

	public void testHTMLTags() throws Exception {
 		if (true) {
 			System.out.println("no HTML tag proposals in core jdoc assist");
 			return;
 		}
		tearDown();
		for (int i= 0; i < HTML_TAGS.length; i++) {
			setUp();
			String tag= HTML_TAGS[i];
			assertTypeJavadocProposal(" * Prefix <" + tag.charAt(0) + "| postfix", "<" + tag, " * Prefix <" + tag + ">| postfix");
			tearDown();
		}
		setUp();
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#getContentType()
	 */
	protected String getContentType() {
		return IJavaPartitions.JAVA_DOC;
	}
}
