/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;

public class RenamingNameSuggestorTests extends TestCase {

	public static Test suite() {
		return new TestSuite(RenamingNameSuggestorTests.class);
	}

	String[] fPrefixes;
	String[] fSuffixes;
	RenamingNameSuggestor fSuggestor;

	private String fHelper(String oldFieldName, String oldTypeName, String newTypeName) {
		return fSuggestor.suggestNewVariableName(fPrefixes, fSuffixes, oldFieldName, oldTypeName, newTypeName);
	}

	private String mHelper(String oldFieldName, String oldTypeName, String newTypeName) {
		return fSuggestor.suggestNewMethodName(oldFieldName, oldTypeName, newTypeName);
	}

	public void mh(String orig, String changed, String oldT, String newT) {
		assertEquals(changed, mHelper(orig, oldT, newT));
	}

	public void mhf(String orig, String oldT, String newT) {
		assertEquals(null, mHelper(orig, oldT, newT));
	}

	public void fh(String orig, String changed, String oldT, String newT) {
		assertEquals(changed, fHelper(orig, oldT, newT));
	}

	public void fhf(String orig, String oldT, String newT) {
		assertEquals(null, fHelper(orig, oldT, newT));
	}

	public void setStrategy(int strategy) {
		fSuggestor= new RenamingNameSuggestor(strategy);
	}

	public void testOnlyPrefix() {

		fPrefixes= new String[] { "f", "q" };
		fSuffixes= new String[0];
		setStrategy(RenamingNameSuggestor.STRATEGY_EXACT);

		// Prefix can, but need not be specified.
		assertEquals("fSomeOtherClass", fHelper("fSomeClass", "SomeClass", "SomeOtherClass"));
		assertEquals("qSomeOtherClass", fHelper("qSomeClass", "SomeClass", "SomeOtherClass"));
		assertEquals("someOtherClass", fHelper("someClass", "SomeClass", "SomeOtherClass"));

		//Interface names
		assertEquals("fNewJavaElement", fHelper("fJavaElement", "IJavaElement", "INewJavaElement"));
		assertEquals("newJavaElement", fHelper("javaElement", "IJavaElement", "INewJavaElement"));

		// Unrelated stuff
		assertNull(fHelper("fSomeClass", "Unrelated", "Unrelated2"));

	}

	public void testPrefixAndSuffix() {

		fPrefixes= new String[] { "f", "q" };
		fSuffixes= new String[] { "Suf1" };
		setStrategy(RenamingNameSuggestor.STRATEGY_EXACT);

		// Suffix and Prefix can, but need not be specified.
		assertEquals("fSomeOtherSuf1", fHelper("fSomeSuf1", "Some", "SomeOther"));
		assertEquals("someOtherSuf1", fHelper("someSuf1", "Some", "SomeOther"));
		assertEquals("fSomeOther", fHelper("fSome", "Some", "SomeOther"));
		assertEquals("someOther", fHelper("some", "Some", "SomeOther"));

		//Interface names
		assertEquals("fNewJavaElementSuf1", fHelper("fJavaElementSuf1", "IJavaElement", "INewJavaElement"));
		assertEquals("newJavaElement", fHelper("javaElement", "IJavaElement", "INewJavaElement"));
	}

	public void testOnlySuffix() {

		fPrefixes= new String[0];
		fSuffixes= new String[] { "Suf1" };
		setStrategy(RenamingNameSuggestor.STRATEGY_EXACT);

		//Suffix can, but need not be specified
		assertEquals("someOtherClassSuf1", fHelper("someClassSuf1", "SomeClass", "SomeOtherClass"));
		assertEquals("someOtherClass", fHelper("someClass", "SomeClass", "SomeOtherClass"));

		//Interface names
		assertEquals("newJavaElementSuf1", fHelper("javaElementSuf1", "IJavaElement", "INewJavaElement"));
		assertEquals("newJavaElement", fHelper("javaElement", "IJavaElement", "INewJavaElement"));

	}

	public void testVeryShortNames() {

		fPrefixes= new String[] { "f", "q" };
		fSuffixes= new String[] { "_" };
		setStrategy(RenamingNameSuggestor.STRATEGY_SUFFIX);

		assertEquals("fB", fHelper("fA", "A", "B"));
		assertEquals("qB", fHelper("qA", "A", "B"));
		assertEquals("b", fHelper("a", "A", "B"));

		assertEquals("b_", fHelper("a_", "A", "B"));

		fh("mAHahAHa", "mBHahAHa", "A", "B"); // match first occurrence

	}

	public void testEmbeddedMatching() {

		fPrefixes= new String[0];
		fSuffixes= new String[0];
		setStrategy(RenamingNameSuggestor.STRATEGY_EMBEDDED);

		mh("getJavaElement", "getNewJavaElement", "JavaElement", "NewJavaElement");
		mh("javaElement", "newJavaElement", "JavaElement", "NewJavaElement");

		mh("createjavaElement", "createnewJavaElement", "JavaElement", "NewJavaElement");
		mh("getJavaElement", "getNewJavaElement", "IJavaElement", "INewJavaElement");

		mhf("createJavaElementcache", "JavaElement", "NewJavaElement");

		// match with "_" or "$" and other non-letters and non-digits at the next hunk
		mh("someClass_pm", "someDifferentClass_pm", "SomeClass", "SomeDifferentClass");
		mh("someClass$$", "someDifferentClass$$", "SomeClass", "SomeDifferentClass");

		// match a second type name
		fh("createelementsecondElement", "createelementsecondMember", "Element", "Member");
		fh("createelementsecondelement", "createelementsecondmember", "Element", "Member");
		fhf("createelementsecondelementnomore", "Element", "Member");
	}

	public void testCamelCaseMatching() {

		fPrefixes= new String[0];
		fSuffixes= new String[0];
		setStrategy(RenamingNameSuggestor.STRATEGY_SUFFIX);

		// only the last camel case match
		mh("getElement", "getMember", "JavaElement", "JavaMember");
		mh("getElement", "getMember", "IJavaElement", "IJavaMember");
		mh("getElement", "getMember", "AVeryLongJavaElement", "AVeryLongJavaMember");

		// two
		mh("getJavaElement", "getJavaMember", "SimpleJavaElement", "SimpleJavaMember");
		mh("getJavaElement", "getGeneralElement", "SimpleJavaElement", "SimpleGeneralElement");

		//three
		mh("getVeryLongJavaElement", "getSomeCompletelyDifferentName", "ExtremelyVeryLongJavaElement", "ExtremelySomeCompletelyDifferentName");

		//some fields
		fh("element", "member", "JavaElement", "JavaMember");
		fh("element", "member", "IJavaElement", "IJavaMember");

		fh("javaElement", "javaMember", "JavaElement", "JavaMember");
		fh("cachedJavaElement", "cachedJavaMember", "JavaElement", "JavaMember");

		fh("javaElement", "nonjavaMember", "JavaElement", "NonjavaMember");

		// some methods
		mh("getFreakyClass", "getLast", "FreakyClass", "Last");
		mh("Element", "Member", "SomeFreakyElement", "SomeFreakyMember");
		mh("createMyASTNode", "createMyNode", "MyASTNode", "MyNode");

		// freaky stuff

		mh("getASTNode", "getASTElement", "ASTNode", "ASTElement");
		mh("getASTNode", "getTreeNode", "ASTNode", "TreeNode"); // ;)
		mh("getASTHeaven2", "getNoBrainer", "ASTHeaven2", "NoBrainer");

		fh("java$Element$", "javaElement", "Java$Element$", "JavaElement");

		// suffixes inside the name
		fh("theElementToUse", "theThingToUse", "JavaElement", "JavaThing");

		// only match last hunk
		mh("getJavaSomeElement", "getJavaSomeMember", "JavaElement", "JavaMember");
		mhf("getJavaSome", "JavaElement", "JavaMember");

		// failures

		mhf("getElement", "JavaElementLabel", "JavaMemberLabel");
		mhf("getElementlabel", "JavaElement", "JavaMember");

		fhf("myClass", "A", "B");

		// avoid "silly" name suggestions

		fhf("fElement", "SomeLongElement", "AnotherDifferentElement"); //-> don't suggest renaming fElement to fElement!

	}

	public void testUpperCaseCamelCaseMatching() {

		fPrefixes= new String[] { "f" };
		fSuffixes= new String[0];

		setStrategy(RenamingNameSuggestor.STRATEGY_EXACT);

		// complete uppercase camel case hunks
		fh("fAST", "fAbstractSyntaxTree", "AST", "AbstractSyntaxTree");
		fh("AST", "AbstractSyntaxTree", "AST", "AbstractSyntaxTree");

		// complete uppercase camel case hunks, but lowercased
		fh("fAst", "fAbstractSyntaxTree", "AST", "AbstractSyntaxTree");
		fh("ast", "abstractSyntaxTree", "AST", "AbstractSyntaxTree");
		fh("aST", "abstractSyntaxTree", "AST", "AbstractSyntaxTree");

	    fh("fASTNode", "fAbstractSTNode", "ASTNode", "AbstractSTNode");
	    fh("fASTNode", "fASTreeNode", "ASTNode", "ASTreeNode");

	    /*
		 * do not match:
		 * fh("fAstNode", "fAbstractSTNode", "ASTNode", "AbstractSTNode");
		 * fh("fAstNode", "fAsTreeNode", "ASTNode", "ASTreeNode");
		 *
		 * When changing all-uppercase hunks in exact or embedded mode, it is unclear
		 * which hunk in the new type name corresponds to the custom-lowercased hunk
		 * in the old variable name. Rather than guessing the hunk, we only
		 * proceed hunk-by-hunk in suffix mode.
		 */

	    setStrategy(RenamingNameSuggestor.STRATEGY_EMBEDDED);

	    fh("fAst2", "fAbstractSyntaxTree2", "AST", "AbstractSyntaxTree");
		fh("aST2", "abstractSyntaxTree2", "AST", "AbstractSyntaxTree");

	    setStrategy(RenamingNameSuggestor.STRATEGY_SUFFIX);

		// partial uppercase camel case hunks
		fh("fAST", "fMUST", "PersonalAST", "PersonalMUST");

		// "downgrading" the new hunks
		fh("fAst", "fMust", "PersonalAST", "PersonalMUST");
		fh("fHUNKPowered", "fMONKPowered", "ReallyHUNKPowered", "ReallyMONKPowered");

		fh("fHunkPowered", "fMonkPowered", "ReallyHUNKPowered", "ReallyMONKPowered");
		fh("fHunkPowered", "fMonkPowered", "ReallyHUNKPowered", "ReallyMonkPowered");

		fh("fHunkPowered", "fHunk2Powered", "HunkPowered", "Hunk2Powered");
		fh("powered", "powered2", "HunkPOWERED", "MonkPOWERED2");

		// adapted middle hunks
		fh("astNode", "fastNode", "ASTNode", "FASTNode");
		mh("createMyASTNode", "createMySECONDNode", "MyASTNode", "MySECONDNode");
		mh("createMyAstNode", "createMySecondNode", "MyASTNode", "MySECONDNode");

		// some more methods
		mh("createAST", "createAbstractSyntaxTree", "AST", "AbstractSyntaxTree");

		// match new hunks

		fh("fASTNode", "fMyASTNode", "ASTNode", "MyASTNode");
		fh("fAstNode", "fMyAstNode", "ASTNode", "MyASTNode");
		fh("fDifferentAstNode", "fDifferentMyAstNode", "ASTNode", "MyASTNode");
		fh("fDifferentAstNodeToUse", "fDifferentMyAstNodeToUse", "ASTNode", "MyASTNode");

		// propagating lowercased front
		fh("astNode", "myAstNode", "ASTNode", "MyASTNode");

	}

	public void testPluralS() {

		 fPrefixes= new String[0];
		 fSuffixes= new String[0];
		 setStrategy(RenamingNameSuggestor.STRATEGY_EXACT);
		 fh("items", "things", "Item", "Thing");
		 fh("handies", "mobilePhones", "Handy", "MobilePhone");
		 fh("handy", "mobilePhone", "Handy", "MobilePhone");
		 fh("mobilePhones", "handies", "MobilePhone", "Handy");
		 fh("mobilePhone", "handy", "MobilePhone", "Handy");
		 fh("handies", "mandies", "Handy", "Mandy");
		 setStrategy(RenamingNameSuggestor.STRATEGY_EMBEDDED);
		 fh("itemsOnLoan", "thingsOnLoan", "Item", "Thing");
		 fh("itemStuff", "thingStuff", "Item", "Thing"); //-> no plural s!
		 fh("someHandiesOnLoan", "someMobilePhonesOnLoan", "Handy", "MobilePhone");
		 fh("someHandiesOnLoan", "someMandiesOnLoan", "Handy", "Mandy");
		 setStrategy(RenamingNameSuggestor.STRATEGY_SUFFIX);
		 fh("someItemsOnLoan", "someThingsOnLoan", "ASTItem", "NOASTThing");
		 fh("someHandiesOnLoan", "someMandiesOnLoan", "Handy", "Mandy");
		 fh("someHandiesOnLoan", "someMobilePhonesOnLoan", "Handy", "MobilePhone");
		 fh("somePhonesOnLoan", "someHandiesOnLoan", "MobilePhone", "Handy");
	}

}
