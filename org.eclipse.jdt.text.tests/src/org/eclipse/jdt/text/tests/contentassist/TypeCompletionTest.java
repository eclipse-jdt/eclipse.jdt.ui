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
import org.eclipse.jdt.ui.tests.core.ProjectTestSetup;

/**
 * 
 * @since 3.2
 */
public class TypeCompletionTest extends AbstractCompletionTest {
	private static final Class THIS= TypeCompletionTest.class;

	public static Test allTests() {
		return new ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new ProjectTestSetup(test);
	}

	public static Test suite() {
		return allTests();
	}
	
	/*
	 * @see org.eclipse.jdt.text.tests.contentassist.AbstractCompletionTest#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		getJDTUIPrefs().setValue(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES, true);
	}

	public void testJavaLangType() throws Exception {
		assertMethodBodyProposal("S|", "String ", "String|");
	}
	
	public void testImportedType() throws Exception {
		expectImports("import java.util.Random;\n", "import java.util.Random;\n");
		assertMethodBodyProposal("R|", "Random ", "Random|");
	}
	
	public void testAutoImportType() throws Exception {
		expectImports("", "\nimport java.util.Random;\n");
		assertMethodBodyProposal("R|", "Random ", "Random|");
	}
	
	public void testGenericType() throws Exception {
		expectImports("import java.util.List;\n", "import java.util.List;\n");
		assertMethodBodyProposal("L|", "List ", "List<|E|>");
	}
	
	public void testAutoImportGenericType() throws Exception {
		expectImports("", "\nimport java.util.ArrayList;\n");
		assertMethodBodyProposal("A|", "ArrayList ", "ArrayList<|E|>");
	}
	
	public void testGenericTypeParameterGuessingUnambiguos() throws Exception {
		expectImports("import java.util.List;\n", "import java.util.ArrayList;\nimport java.util.List;\n");
		assertMethodBodyProposal("List<String> list= new A|", "ArrayList ", "List<String> list= new ArrayList<String>|");
	}
	
	public void testGenericTypeParameterGuessingExtends() throws Exception {
		expectImports("import java.util.List;\n", "import java.util.ArrayList;\nimport java.util.List;\n");
		assertMethodBodyProposal("List<? extends Number> list= new A|", "ArrayList ", "List<? extends Number> list= new ArrayList<|Number|>");
	}
	
	public void testGenericTypeParameterGuessingSuper() throws Exception {
		expectImports("import java.util.List;\n", "import java.util.ArrayList;\nimport java.util.List;\n");
		assertMethodBodyProposal("List<? super Number> list= new A|", "ArrayList ", "List<? super Number> list= new ArrayList<|E|>");
	}
	
	public void testGenericTypeParameterGuessingMixed() throws Exception {
		expectImports("import java.util.Map;\n", "import java.util.HashMap;\nimport java.util.Map;\n");
		assertMethodBodyProposal("Map<String, ? extends Number> list= new H|", "HashMap ", "Map<String, ? extends Number> list= new HashMap<String, |Number|>");
	}
	
}
