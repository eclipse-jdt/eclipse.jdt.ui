package org.eclipse.jdt.ui.nls.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.ICodeCompletionRequestor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageDeclaration;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChange;
import org.eclipse.jdt.internal.core.refactoring.text.ITextBufferChangeCreator;
import org.eclipse.jdt.internal.core.nls.model.NLSRefactoring;

public class NLSRefactoringTester extends TestCase {

	/**
	 * Constructor for NLSRefactoringTester
	 */
	public NLSRefactoringTester(String name) {
		super(name);
	}

	public static void main (String[] args) {
		junit.textui.TestRunner.run (suite());
	}
	
	public static Test suite() {
		return new TestSuite(NLSRefactoringTester.class);
	}
	
	private void testRemoveQuotes(String in, String expected){
		assertEquals("remove quotes", expected, NLSRefactoring.removeQuotes(in));
	}
	
	public void test0(){
		testRemoveQuotes("\"x\"", "x");
	}
	
	public void test1(){
		testRemoveQuotes("\"\"", "");	}
}

