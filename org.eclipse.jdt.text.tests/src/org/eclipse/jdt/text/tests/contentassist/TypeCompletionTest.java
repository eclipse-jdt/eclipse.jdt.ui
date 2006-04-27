/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.core.JavaCore;

/**
 * 
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
		assertMethodBodyProposal("Random r= new R|", "Random ", "Random r= new Random|"); // fills RHS cache
		
		assertMethodBodyProposal("Random r= new |", "Random ", "Random r= new Random|"); // tests RHS cache
	}

	public void testInnerImportedType() throws Exception {
		if (true) {
			return; // TODO disabled as the proposal does not show up relieably 
		}
		addImport("java.security.KeyStore");
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
		assertMethodBodyProposal("Map.E|", "Map<K,V>.Entry", "Map.Entry<|K|, V>");
	}
	
	public void testInnerTypeOfGenericOuter() throws Exception {
		addMembers("static class Outer<E> { class Inner {} }");
		assertMethodBodyProposal("Outer<String>.I|", "Outer<java.lang.String>.Inner", "Outer<String>.Inner|");
	}

	public void testInnerTypeOfGenericOuter2() throws Exception {
		if (true) {
			return; // TODO disabled as the proposal does not show up relieably 
		}
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
		assertMethodBodyProposal("List<String> list= new A|", "ArrayList ", "List<String> list= new ArrayList<String>|");
	}
	
	public void testGenericParameterGuessingExtends() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? extends Number> list= new A|", "ArrayList ", "List<? extends Number> list= new ArrayList<|Number|>");
	}
	
	public void testGenericParameterGuessingSuper() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? super Number> list= new A|", "ArrayList ", "List<? super Number> list= new ArrayList<|E|>");
	}
	
	public void testGenericParameterGuessingMixed() throws Exception {
		addImport("java.util.Map");
		expectImport("java.util.HashMap");
		expectImport("java.util.Map");
		assertMethodBodyProposal("Map<String, ? extends Number> list= new H|", "HashMap ", "Map<String, ? extends Number> list= new HashMap<String, |Number|>");
	}
	
	public void testNoCamelCase() throws Exception {
		assertNoMethodBodyProposals("SB|", "StringBuffer ");
	}
	
	public void testCamelCase() throws Exception {
		if (true) {
			return; // TODO disabled as the proposal does not show up relieably 
		}
		setCoreOption(JavaCore.CODEASSIST_CAMEL_CASE_MATCH, JavaCore.ENABLED);
		
		// first trigger availability in the model
		assertMethodBodyProposal("StringBuffer|", "StringBuffer ", "StringBuffer|");
		
		assertMethodBodyProposal("SB|", "StringBuffer ", "StringBuffer|");
	}

	public void testConstructorParentheses() throws Exception {
		setTrigger('(');
		assertMethodBodyProposal("StringBuf|", "StringBuffer ", "StringBuffer(|)");
	}
}
