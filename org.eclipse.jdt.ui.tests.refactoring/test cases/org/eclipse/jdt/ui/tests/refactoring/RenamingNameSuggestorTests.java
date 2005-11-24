package org.eclipse.jdt.ui.tests.refactoring;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor;

public class RenamingNameSuggestorTests extends TestCase {
	
	public static Test suite() {
		return new TestSuite(RenamingNameSuggestorTests.class);
	}
	
	String[] prefixes;
	String[] suffixes;

	private String fHelper(String oldFieldName, String oldTypeName, String newTypeName) {
		
		return new RenamingNameSuggestor().suggestNewVariableName(prefixes, suffixes, oldFieldName, oldTypeName, newTypeName);
	}
	
	private String mHelper(String oldFieldName, String oldTypeName, String newTypeName) {
		
		return new RenamingNameSuggestor().suggestNewMethodName(oldFieldName, oldTypeName, newTypeName);
	}
	
	public void mh(String orig, String changed, String oldT, String newT) {
		assertEquals(changed, mHelper(orig, oldT, newT));
	}
	
	public void mhf(String orig, String oldT, String newT) {
		assertNull(mHelper(orig, oldT, newT));
	}
	
	public void fh(String orig, String changed, String oldT, String newT) {
		assertEquals(changed, fHelper(orig, oldT, newT));
	}
	
	public void fhf(String orig, String oldT, String newT) {
		assertNull(fHelper(orig, oldT, newT));
	}

	public void testOnlyPrefix() {
		
		prefixes= new String[] { "f", "q" };
		suffixes= new String[0];
		
		// Prefix can, but need not be specified.
		assertEquals("fSomeOtherClass", fHelper("fSomeClass", "SomeClass", "SomeOtherClass"));
		assertEquals("qSomeOtherClass", fHelper("qSomeClass", "SomeClass", "SomeOtherClass"));
		assertEquals("someOtherClass", fHelper("someClass", "SomeClass", "SomeOtherClass"));
		
		//Interface names
		assertEquals("fINewJavaElement", fHelper("fIJavaElement", "IJavaElement", "INewJavaElement"));
		assertEquals("fNewJavaElement", fHelper("fJavaElement", "IJavaElement", "INewJavaElement"));
		assertEquals("newJavaElement", fHelper("javaElement", "IJavaElement", "INewJavaElement"));
		assertEquals("iNewJavaElement", fHelper("iJavaElement", "IJavaElement", "INewJavaElement"));
		
		// Unrelated stuff
		assertNull(fHelper("fSomeClass", "Unrelated", "Unrelated2"));
		
	}

	public void testPrefixAndSuffix() {

		prefixes= new String[] { "f", "q" };
		suffixes= new String[] { "Suf1" };

		// Suffix and Prefix can, but need not be specified.
		assertEquals("fSomeOtherSuf1", fHelper("fSomeSuf1", "Some", "SomeOther"));
		assertEquals("someOtherSuf1", fHelper("someSuf1", "Some", "SomeOther"));
		assertEquals("fSomeOther", fHelper("fSome", "Some", "SomeOther"));
		assertEquals("someOther", fHelper("some", "Some", "SomeOther"));
		
		//Interface names
		assertEquals("fINewJavaElementSuf1", fHelper("fIJavaElementSuf1", "IJavaElement", "INewJavaElement"));
		assertEquals("fNewJavaElementSuf1", fHelper("fJavaElementSuf1", "IJavaElement", "INewJavaElement"));
		assertEquals("newJavaElement", fHelper("javaElement", "IJavaElement", "INewJavaElement"));
		assertEquals("iNewJavaElement", fHelper("iJavaElement", "IJavaElement", "INewJavaElement"));
	}
	
	public void testOnlySuffix() {
		
		prefixes= new String[0];
		suffixes= new String[] { "Suf1" };
		
		//Suffix can, but need not be specified
		assertEquals("someOtherClassSuf1", fHelper("someClassSuf1", "SomeClass", "SomeOtherClass"));
		assertEquals("someOtherClass", fHelper("someClass", "SomeClass", "SomeOtherClass"));
		
		//Interface names
		assertEquals("iNewJavaElementSuf1", fHelper("iJavaElementSuf1", "IJavaElement", "INewJavaElement"));
		assertEquals("newJavaElementSuf1", fHelper("javaElementSuf1", "IJavaElement", "INewJavaElement"));
		assertEquals("newJavaElement", fHelper("javaElement", "IJavaElement", "INewJavaElement"));
		assertEquals("iNewJavaElement", fHelper("iJavaElement", "IJavaElement", "INewJavaElement"));
		
	}

	public void testVeryShortNames() {
		
		prefixes= new String[] { "f", "q" };
		suffixes= new String[] { "_" };
		
		assertEquals("fB", fHelper("fA", "A", "B"));
		assertEquals("qB", fHelper("qA", "A", "B"));
		assertEquals("b", fHelper("a", "A", "B"));
		
		assertEquals("b_", fHelper("a_", "A", "B"));
	
	}
	
	public void testEmbeddedMatching() {
		
		prefixes= new String[0];
		suffixes= new String[0];
		
		mh("getJavaElement", "getNewJavaElement", "JavaElement", "NewJavaElement");
		mh("javaElement", "newJavaElement", "JavaElement", "NewJavaElement");
		
		mh("createjavaElement", "createnewJavaElement", "JavaElement", "NewJavaElement");
		mh("getJavaElement", "getNewJavaElement", "IJavaElement", "INewJavaElement");
		
		mhf("createJavaElementcache", "JavaElement", "NewJavaElement");
		
		// match with "_" or "$" and other non-letters and non-digits at the next hunk
		mh("someClass_pm", "someDifferentClass_pm", "SomeClass", "SomeDifferentClass");
		mh("someClass$$", "someDifferentClass$$", "SomeClass", "SomeDifferentClass");
	}
	
	public void testCamelCaseMatching() {
		
		prefixes= new String[0];
		suffixes= new String[0];

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
		
		// freaky stuff
		
		mh("getASTNode", "getASTElement", "ASTNode", "ASTElement");
		mh("getASTNode", "getTreeNode", "ASTNode", "TreeNode"); // ;)
		mh("getASTHeaven2", "getNoBrainer", "ASTHeaven2", "NoBrainer");
		
		fh("java$Element$", "javaElement", "Java$Element$", "JavaElement");
		
		
		// failures

		mhf("getElement", "JavaElementLabel", "JavaMemberLabel");
		mhf("getElementlabel", "JavaElement", "JavaMember");
		
		fhf("myClass", "A", "B");
		
		// avoid "silly" name suggestions
		
		fhf("fElement", "SomeLongElement", "AnotherDifferentElement"); //-> don't suggest renaming fElement to fElement!
		
	}
	
}
