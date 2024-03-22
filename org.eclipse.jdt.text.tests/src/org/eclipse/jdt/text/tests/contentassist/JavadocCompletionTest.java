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
package org.eclipse.jdt.text.tests.contentassist;

import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.IJavaPartitions;

/*
 * This test tests only <= 1.5 source level tags.
 */
public class JavadocCompletionTest extends AbstractCompletionTest {
	private static final String METHOD=
			"""
			public int method(int param) {
				return 0;
			}
		""";
	private static final String FIELD= "	public int fField\n";

	private static final String[] TYPE_BLOCK_TAGS= {"@see", "@since", "@deprecated", "@serial", "@author", "@version", "@param", };
	private static final String[] METHOD_BLOCK_TAGS= {"@see", "@since", "@deprecated", "@param", "@return", "@throws", "@exception", "@serialData", };
	private static final String[] FIELD_BLOCK_TAGS= {"@see", "@since", "@deprecated", "@serial", "@serialField", };
	private static final String[] TYPE_INLINE_TAGS= {"@docRoot", "@link", "@linkplain", "@value", "@code", "@literal", };
	private static final String[] METHOD_INLINE_TAGS= {"@docRoot", "@inheritDoc", "@link", "@linkplain", "@value", "@code", "@literal", };
	private static final String[] FIELD_INLINE_TAGS= {"@docRoot", "@link", "@linkplain", "@value", "@code", "@literal", };
	private static final String[] HTML_TAGS= {"b", "blockquote", "br", "code", "dd", "dl", "dt", "em", "hr", "h1", "h2", "h3", "h4", "h5", "h6", "i", "li", "nl", "ol", "p", "pre", "q", "td", "th", "tr", "tt", "ul",};

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	@Override
	public void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, false);
	}

	@Test
	public void testSeeType() throws Exception {
		assertTypeJavadocProposal(" * @see List|", "List ", " * @see java.util.List|");
	}

	@Test
	public void testSeeTypeImportsOn() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * @see List|", "List ", " * @see List|");
	}

	@Test
	public void testSeeTypeJavaLang() throws Exception {
		assertTypeJavadocProposal(" * @see Str|", "String ", " * @see String|");
	}

	@Test
	public void testSeeImportedQualifiedType() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * @see java.util.Lis|", "List ", " * @see java.util.List|");
	}

	@Test
	public void testSeeImportedType() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * @see Lis|", "List ", " * @see List|");
	}

	@Test
	public void testSeeImportedTypeImportsOn() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		assertTypeJavadocProposal(" * @see Lis|", "List ", " * @see List|");
	}

	@Test
	public void testSeeTypeSameType() throws Exception {
		assertTypeJavadocProposal(" * @see Comple|", "Completion_testSeeTypeSameType", " * @see Completion_testSeeTypeSameType|");
	}

	@Test
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
	@Test
	public void testSeeMethod() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#siz|", "size(", " * @see java.util.List#size()|");
	}

	@Ignore("JavadocCompletionTest.testSeeMethodWithoutImport() - no best-effort imports with Core completion")
	@Test
	public void testSeeMethodWithoutImport() throws Exception {
		assertTypeJavadocProposal(" * @see List#siz|", "size(", " * @see List#size()|");
	}

	@Test
	public void testSeeMethodWithParam() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#ge|", "get(", " * @see java.util.List#get(int)|");
	}

	@Test
	public void testSeeMethodWithTypeVariableParameter() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.List#ad|", "add(O", " * @see java.util.List#add(Object)|");
	}

	@Test
	public void testSeeMethodLocal() throws Exception {
		addMembers(METHOD);
		assertTypeJavadocProposal(" * @see #me|", "met", " * @see #method(int)|");
	}

	@Test
	public void testSeeConstant() throws Exception {
		assertTypeJavadocProposal(" * @see java.util.Collections#|", "EMPTY_LI", " * @see java.util.Collections#EMPTY_LIST|");
	}

	@Test
	public void testLinkType() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link List|", "List ", " * Prefix {@link java.util.List|");
	}
	@Test
	public void testLinkTypeClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link List|}", "List ", " * Prefix {@link java.util.List|}");
	}
	@Test
	public void testDirectLinkType() throws Exception {
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link java.util.List}|");
	}
	@Ignore("BUG_113544 not testing autoclosing behavior, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544")
	@Test
	public void testDirectLinkTypeNoAutoClose() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, false);
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link java.util.List|");
	}

	@Test
	public void testDirectLinkTypeImportsOn() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List}|");
	}

	@Test
	public void testDirectLinkTypeExistingImport() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List}|");
	}

	@Test
	public void testDirectLinkImportsOnExistingImport() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List}|");
	}

	@Test
	public void testDirectLinkImportsOnExistingImportQualifiedPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		addImport("java.util.List");
		expectImport("java.util.List");
		assertTypeJavadocProposal(" * Prefix java.util.List|", "{@link List}", " * Prefix {@link java.util.List}|");
	}

	@Test
	public void testDirectLinkImportsOnExistingImportCamelCase() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		addImport("java.text.DateFormat");
		expectImport("java.text.DateFormat");
		assertTypeJavadocProposal(" * Prefix DF|", "{@link DateFormat}", " * Prefix {@link DateFormat}|");
	}

	@Test
	public void testDirectLinkImportsOnExistingImportCamelCaseQualifiedPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		addImport("java.text.DateFormat");
		expectImport("java.text.DateFormat");
		assertTypeJavadocProposal(" * Prefix java.text.DF|", "{@link DateFormat}", " * Prefix {@link java.text.DateFormat}|");
	}

	@Ignore("BUG_113544 not testing autoclosing behavior, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=113544")
	@Test
	public void testDirectLinkTypeNoAutoCloseImportsOn() throws Exception {
		expectImport("java.util.List");
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, true);
		getJDTUIPrefs().setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, false);
		assertTypeJavadocProposal(" * Prefix List|", "{@link List}", " * Prefix {@link List|");
	}

	@Test
	public void testLinkTypeJavaLang() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Str|", "String ", " * Prefix {@link String|");
	}
	@Test
	public void testLinkTypeJavaLangClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Str|}", "String ", " * Prefix {@link String|}");
	}

	@Test
	public void testLinkTypeSameType() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Comple|", "Completion_testLinkTypeSameType", " * Prefix {@link Completion_testLinkTypeSameType|");
	}
	@Test
	public void testLinkTypeSameTypeClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link Comple|}", "Completion_testLinkTypeSameTypeClosed", " * Prefix {@link Completion_testLinkTypeSameTypeClosed|}");
	}

	@Ignore("JavadocCompletionTest.testLinkMethodWithoutImport() - no best-effort imports with Core completion")
	@Test
	public void testLinkMethodWithoutImport() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link List#siz|", "size(", " * Prefix {@link List#size()|");
	}

	@Test
	public void testLinkMethod() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#siz|", "size(", " * Prefix {@link java.util.List#size()|");
	}

	@Test
	public void testLinkMethodLocal() throws Exception {
		addMembers(METHOD);
		assertTypeJavadocProposal(" * {@link #me|", "met", " * {@link #method(int)|");
	}

	@Test
	public void testLinkMethodClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#siz|}", "size(", " * Prefix {@link java.util.List#size()|}");
	}

	@Test
	public void testLinkMethodWithParam() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ge|", "get(", " * Prefix {@link java.util.List#get(int)|");
	}

	@Test
	public void testLinkMethodWithParamNoOverwrite() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add} postfix", "clear", " * Prefix {@link java.util.List#clear()|add} postfix");
	}

	@Test
	public void testLinkMethodWithParamNoOverwriteWithParams() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add(int, Object)} postfix", "clear", " * Prefix {@link java.util.List#clear()|add(int, Object)} postfix");
	}

	@Test
	public void testLinkMethodWithParamOverwriteNoPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add} postfix", "clear", " * Prefix {@link java.util.List#clear()|} postfix");
	}

	@Test
	public void testLinkMethodWithParamOverwrite() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#g|et} postfix", "get(", " * Prefix {@link java.util.List#get(int)|} postfix");
	}

	@Test
	public void testLinkMethodWithParamOverwriteWithParamsNoPrefix() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#|add(int, Object)} postfix", "clear", " * Prefix {@link java.util.List#clear()|} postfix");
	}

	@Test
	public void testLinkMethodWithParamOverwriteWithParams() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_INSERT_COMPLETION, false);
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#g|et(long)} postfix", "get(", " * Prefix {@link java.util.List#get(int)|} postfix");
	}

	@Test
	public void testLinkMethodWithParamClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ge|}", "get(", " * Prefix {@link java.util.List#get(int)|}");
	}

	@Test
	public void testLinkMethodWithTypeVariableParameter() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ad|", "add(O", " * Prefix {@link java.util.List#add(Object)|");
	}
	@Test
	public void testLinkMethodWithTypeVariableParameterClosed() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.List#ad|}", "add(O", " * Prefix {@link java.util.List#add(Object)|}");
	}

	@Test
	public void testLinkConstant() throws Exception {
		assertTypeJavadocProposal(" * Prefix {@link java.util.Collections#|", "EMPTY_LI", " * Prefix {@link java.util.Collections#EMPTY_LIST|");
	}

	@Test
	public void testFieldReference() throws Exception {
		assertTypeJavadocProposal(" * Prefix java.util.Collections#|", "{@link java.util.Collections#EMPTY_LI", " * Prefix {@link java.util.Collections#EMPTY_LIST}|");
	}

	@Test
	public void testFieldValueReference() throws Exception {
		assertTypeJavadocProposal(" * Prefix java.util.Collections#|", "{@value java.util.Collections#EMPTY_LI", " * Prefix {@value java.util.Collections#EMPTY_LIST}|");
	}

	@Test
	public void testMethodReference() throws Exception {
		assertTypeJavadocProposal(" * Prefix java.util.Collections#|", "{@link java.util.Collections#toSt", " * Prefix {@link java.util.Collections#toString()}|");
	}

	@Test
	public void testTypeBlockTags() throws Exception {
		tearDown();
		for (String t : TYPE_BLOCK_TAGS) {
			setUp();
			String tag= t;
			assertTypeJavadocProposal(" * @|", tag, " * " + tag);
			tearDown();
		}
		setUp();
	}

	@Test
	public void testMethodBlockTags() throws Exception {
		tearDown();
		for (String t : METHOD_BLOCK_TAGS) {
			setUp();
			addMembers(METHOD);
			String tag= t;
			assertMemberJavadocProposal(" * @|", tag, " * " + tag);
			tearDown();
		}
		setUp();
	}

	@Test
	public void testFieldBlockTags() throws Exception {
		tearDown();
		for (String t : FIELD_BLOCK_TAGS) {
			setUp();
			addMembers(FIELD);
			String tag= t;
			assertMemberJavadocProposal(" * @|", tag, " * " + tag);
			tearDown();
		}
		setUp();
	}

 	public void testNoInlineAsBlockTags() throws Exception {
 		tearDown();
		for (String t : TYPE_INLINE_TAGS) {
			setUp();
			String tag= t;
			assertNoMethodBodyProposals(" * @|", tag);
			tearDown();
		}
		setUp();
	}

	@Test
	public void testTypeInlineTags() throws Exception {
 		tearDown();
		for (String t : TYPE_INLINE_TAGS) {
			setUp();
			String tag= t;
			assertTypeJavadocProposal(" * {@|", "{" + tag + "}", " * {" + tag + "|}");
			tearDown();
		}
		setUp();
	}

	@Test
	public void testMethodInlineTags() throws Exception {
		tearDown();
		for (String t : METHOD_INLINE_TAGS) {
			setUp();
			addMembers(METHOD);
			String tag= t;
			assertMemberJavadocProposal(" * {@|", "{" + tag + "}", " * {" + tag + "|}");
			tearDown();
		}
		setUp();
	}

	@Test
	public void testFieldInlineTags() throws Exception {
		tearDown();
		for (String t : FIELD_INLINE_TAGS) {
			setUp();
			addMembers(FIELD);
			String tag= t;
			assertMemberJavadocProposal(" * {@|", "{" + tag + "}", " * {" + tag + "|}");
			tearDown();
		}
		setUp();
	}

	@Test
	public void testNoBlockAsInlineTags() throws Exception {
		tearDown();
		for (String tag : TYPE_BLOCK_TAGS) {
			setUp();
			assertNoMethodBodyProposals(" * {@|", tag);
			tearDown();
		}
		setUp();
	}

	@Ignore("no HTML tag proposals in core jdoc assist")
	@Test
	public void testHTMLTags() throws Exception {
		tearDown();
		for (String t : HTML_TAGS) {
			setUp();
			String tag= t;
			assertTypeJavadocProposal(" * Prefix <" + tag.charAt(0) + "| postfix", "<" + tag, " * Prefix <" + tag + ">| postfix");
			tearDown();
		}
		setUp();
	}

	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#getContentType()
	 */
	@Override
	protected String getContentType() {
		return IJavaPartitions.JAVA_DOC;
	}
}
