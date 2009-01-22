/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.ui.PreferenceConstants;


/**
 * @since 3.2
 */
public class TypeCompletionTest extends AbstractCompletionTest {

	private static final Class THIS= TypeCompletionTest.class;

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
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
		getJDTUIPrefs().setValue(PreferenceConstants.EDITOR_CLOSE_BRACKETS, true);
	}

	public void testJavaLang() throws Exception {
		assertMethodBodyProposal("S|", "String ", "String|");
	}

	public void testImported() throws Exception {
		addImport("java.util.Random");
		expectImport("java.util.Random");
		assertMethodBodyProposal("R|", "Random ", "Random|");
	}

	public void testAutoImport() throws Exception {
		expectImport("java.util.Random");
		assertMethodBodyProposal("R|", "Random ", "Random|");
	}

	public void testNoAutoImportForQualifiedPrefix() throws Exception {
		assertMethodBodyProposal("java.util.R|", "Random ", "java.util.Random|");
	}

	public void testNoQualifierRemovalForQualifiedPrefix() throws Exception {
		addImport("java.util.Random");
		expectImport("java.util.Random");
		assertMethodBodyProposal("java.util.R|", "Random ", "java.util.Random|");
	}

	public void testAutoQualify() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, false);
		assertMethodBodyProposal("R|", "Random ", "java.util.Random|");
	}

	public void testNoAutoQualifyWithImport() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, false);
		addImport("java.util.Random");
		expectImport("java.util.Random");
		assertMethodBodyProposal("R|", "Random ", "Random|");
	}

	public void testNoQualifierRemovalWithImport() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_ADDIMPORT, false);
		addImport("java.util.Random");
		expectImport("java.util.Random");
		assertMethodBodyProposal("java.util.R|", "Random ", "java.util.Random|");
	}

	public void testAutoImportZeroPrefix() throws Exception {
		addImport("java.util.Random");
		expectImport("java.util.Random");
		assertMethodBodyProposal("Random r= new R|", "Random()", "Random r= new Random()|"); // fills RHS cache

		assertMethodBodyProposal("Random r= new |", "Random ", "Random r= new Random|"); // tests RHS cache
	}

	public void testInnerImportedType() throws Exception {
		waitBeforeCompleting(true);
		addImport("java.security.KeyStore");
		expectImport("java.security.KeyStore");
		expectImport("java.security.KeyStore.Entry");
		assertMethodBodyProposal("Entry|", "Entry - java.security.KeyStore", "Entry|");
    }

	public void testInnerTypeOuterImported() throws Exception {
		addImport("java.security.KeyStore");
		expectImport("java.security.KeyStore");
		assertMethodBodyProposal("KeyStore.E|", "Entry", "KeyStore.Entry|");
	}

	public void testGenericInnerTypeOuterImported() throws Exception {
		addImport("java.util.Map");
		expectImport("java.util.Map");
		assertMethodBodyProposal("Map.E|", "Entry", "Map.Entry<|K|, V>");
	}

	public void testInnerTypeOfGenericOuter() throws Exception {
		addMembers("static class Outer<E> { class Inner {} }");
		assertMethodBodyProposal("Outer<String>.I|", "Outer<java.lang.String>.Inner", "Outer<String>.Inner|");
	}

	public void testInnerTypeOfGenericOuter2() throws Exception {
		addMembers("static class Outer<E> { class Inner {} }");
		expectImport("test1.Completion_" + getName() + ".Outer.Inner");
		assertMethodBodyProposal("Inner|", "Completion_" + getName() +"<T>.Outer<E>.Inner", "Inner|");
	}

	public void testInnerTypeOfGenericOuterImported() throws Exception {
		addMembers("static class Outer<E> { class Inner {} }");
		addImport("test1.Completion_" + getName() + ".Outer");
		expectImport("test1.Completion_" + getName() + ".Outer");
		assertMethodBodyProposal("Outer<String>.I|", "Outer<java.lang.String>.Inner", "Outer<String>.Inner|");
	}

	public void testInnerTypeOfGenericOuterImported2() throws Exception {
		addMembers("static class Outer<E> { class Inner {} }");
		addImport("test1.Completion_" + getName() + ".Outer.Inner");
		expectImport("test1.Completion_" + getName() + ".Outer.Inner");
		assertMethodBodyProposal("Inner|", "Completion_" + getName() +"<T>.Outer<E>.Inner", "Inner|");
	}

	public void testGeneric() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertMethodBodyProposal("L|", "List ", "List<|E|>");
	}

	public void testAutoImportGeneric() throws Exception {
		expectImport("java.util.ArrayList");
		assertMethodBodyProposal("A|", "ArrayList ", "ArrayList<|E|>");
	}

	public void testGenericParameterGuessingUnambiguos() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<String> list= new A|", "ArrayList()", "List<String> list= new ArrayList<String>()|");
	}

	public void testGenericParameterGuessingExtends() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? extends Number> list= new A|", "ArrayList()", "List<? extends Number> list= new ArrayList<Number>()|");
	}

	public void testGenericParameterGuessingSuper() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? super Number> list= new A|", "ArrayList()", "List<? super Number> list= new ArrayList<E>()|");
	}

	public void testGenericParameterGuessingMixed() throws Exception {
		addImport("java.util.Map");
		expectImport("java.util.HashMap");
		expectImport("java.util.Map");
		assertMethodBodyProposal("Map<String, ? extends Number> list= new H|", "HashMap()", "Map<String, ? extends Number> list= new HashMap<String, Number>()|");
	}

	public void testNoCamelCase() throws Exception {
		assertNoMethodBodyProposals("SB|", "StringBuffer ");
	}

	public void testCamelCase() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);

		// first trigger availability in the model
		assertMethodBodyProposal("StringBuffer|", "StringBuffer ", "StringBuffer|");

		assertMethodBodyProposal("SB|", "StringBuffer ", "StringBuffer|");
	}

	public void testConstructorParentheses() throws Exception {
		setTrigger('(');
		assertMethodBodyProposal("StringBuf|", "StringBuffer ", "StringBuffer(|)");
	}

	public void testIncrementalInsertion() throws Exception {
	    getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
	    getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);

	    assertMethodBodyIncrementalCompletion("Strin|", "String|");
    }

	public void testNoIncrementalInsertion() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);

		assertMethodBodyIncrementalCompletion("String|", "String|");
	}

	public void testIncrementalInsertionPrefixCorrection() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);

		assertMethodBodyIncrementalCompletion("StRiN|", "String|");
	}

	public void testNoIncrementalInsertionPrefixCorrection() throws Exception {
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);

		assertMethodBodyIncrementalCompletion("StRiNg|", "String|");
	}

	public void testNoIncrementalInsertionCamelCase() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);

		// no auto-insertion of IOException, as IndexOutOfBoundsException is a valid camel case match
		assertMethodBodyIncrementalCompletion("IO|", "IO|");
	}

	public void testIncrementalInsertionCamelCase() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);
		expectImport("java.util.jar.JarEntry");

		assertMethodBodyIncrementalCompletion("JaEn|", "JarEntry|");
	}

	public void testNoIncrementalInsertionCamelCase2() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);

		// JarEntry vs. JarException - note that the common prefix 'Jar' is not inserted
		// as the common camelCase prefix is 'JaE'
		assertMethodBodyIncrementalCompletion("JaE|", "JaE|");
	}

	public void testIncrementalInsertionCamelCase2() throws Exception {
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_PREFIX_COMPLETION, true);
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_AUTOINSERT, true);

		// no auto-insertion, but prefix-completion of IOException, as InvalidObjectException is a valid camel case match
		assertMethodBodyIncrementalCompletion("IOExce|", "IOException|");
	}

	public void testBug182468() throws Exception {
		IPackageFragmentRoot src= (IPackageFragmentRoot)CompletionTestSetup.getTestPackage().getParent();

		IPackageFragment package1= src.createPackageFragment("package1", true, null);
		package1.createCompilationUnit("AClass.java", "package " + package1.getElementName() + "; public class AClass {}", true, null);

		IPackageFragment package2= src.createPackageFragment("package2", true, null);
		package1.createCompilationUnit("AClass.java", "package " + package2.getElementName() + "; public class AClass {}", true, null);

		waitBeforeCompleting(true);

		addImport(package1.getElementName() + ".AClass");
		expectImport(package1.getElementName() + ".AClass");
		assertMethodBodyProposal("new AClass|", "AClass() - " + package2.getElementName(), "new " + package2.getElementName() + ".AClass()");
	}
}
