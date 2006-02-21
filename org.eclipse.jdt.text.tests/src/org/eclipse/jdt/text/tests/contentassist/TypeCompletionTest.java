/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

	public void testJavaLangType() throws Exception {
		assertMethodBodyProposal("S|", "String ", "String|");
	}
	
	public void testImportedType() throws Exception {
		addImport("java.util.Random");
		expectImport("java.util.Random");
		assertMethodBodyProposal("R|", "Random ", "Random|");
	}
	
	public void testAutoImportType() throws Exception {
		expectImport("java.util.Random");
		assertMethodBodyProposal("R|", "Random ", "Random|");
	}
	
	public void testGenericType() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.List");
		assertMethodBodyProposal("L|", "List ", "List<|E|>");
	}
	
	public void testAutoImportGenericType() throws Exception {
		expectImport("java.util.ArrayList");
		assertMethodBodyProposal("A|", "ArrayList ", "ArrayList<|E|>");
	}
	
	public void testGenericTypeParameterGuessingUnambiguos() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<String> list= new A|", "ArrayList ", "List<String> list= new ArrayList<String>|");
	}
	
	public void testGenericTypeParameterGuessingExtends() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? extends Number> list= new A|", "ArrayList ", "List<? extends Number> list= new ArrayList<|Number|>");
	}
	
	public void testGenericTypeParameterGuessingSuper() throws Exception {
		addImport("java.util.List");
		expectImport("java.util.ArrayList");
		expectImport("java.util.List");
		assertMethodBodyProposal("List<? super Number> list= new A|", "ArrayList ", "List<? super Number> list= new ArrayList<|E|>");
	}
	
	public void testGenericTypeParameterGuessingMixed() throws Exception {
		addImport("java.util.Map");
		expectImport("java.util.HashMap");
		expectImport("java.util.Map");
		assertMethodBodyProposal("Map<String, ? extends Number> list= new H|", "HashMap ", "Map<String, ? extends Number> list= new HashMap<String, |Number|>");
	}
	
	public void testNoCamelCase() throws Exception {
		assertNoMethodBodyProposals("SB|", "StringBuffer ");
	}
	
	public void testConstructorParentheses() throws Exception {
		setTrigger('(');
		assertMethodBodyProposal("StringBuf|", "StringBuffer ", "StringBuffer(|)");
	}
}
