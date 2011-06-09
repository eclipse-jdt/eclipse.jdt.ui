/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import java.util.Arrays;
import java.util.Comparator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java17ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.ASTProvider;
import org.eclipse.jdt.internal.ui.search.ExceptionOccurrencesFinder;
import org.eclipse.jdt.internal.ui.search.IOccurrencesFinder;
import org.eclipse.jdt.internal.ui.search.IOccurrencesFinder.OccurrenceLocation;
import org.eclipse.jdt.internal.ui.search.MethodExitsFinder;

/**
 * Tests the Java Editor's occurrence marking feature.
 */
public class MarkOccurrenceTest17 extends TestCase {
	private static final Class THIS= MarkOccurrenceTest17.class;

	public static Test suite() {
		return new Java17ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java17ProjectTestSetup(test);
	}

	private ASTParser fParser;

	private IOccurrencesFinder fFinder;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		fParser= ASTParser.newParser(ASTProvider.SHARED_AST_LEVEL);

		fJProject1= Java17ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, true);
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_IMPLEMENTORS, true);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java17ProjectTestSetup.getDefaultClasspath());
	}

	private OccurrenceLocation[] getHighlights(StringBuffer source, int offset, int length) throws Exception {
		CompilationUnit root= createCompilationUnit(source);
		String errorString= fFinder.initialize(root, offset, length);
		assertNull(errorString, errorString);
		return fFinder.getOccurrences();
	}

	private CompilationUnit createCompilationUnit(StringBuffer source) throws JavaModelException {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", source.toString(), false, null);
		fParser.setSource(cu);
		fParser.setResolveBindings(true);
		return (CompilationUnit)fParser.createAST(null);
	}

	private void checkSelection(StringBuffer s, int offset, int length, OccurrenceLocation[] expected) throws Exception {
		OccurrenceLocation[] selectedNodes= getHighlights(s, offset, length);
		assertEquals("number of selections", expected.length, selectedNodes.length);
		sortByStartIndex(selectedNodes);
		sortByStartIndex(expected);
		for (int i= 0; i < selectedNodes.length; i++) {
			assertEquals(expected[i].getOffset(), selectedNodes[i].getOffset());
			assertEquals(expected[i].getLength(), selectedNodes[i].getLength());
		}
	}

	private void sortByStartIndex(OccurrenceLocation[] OccurrenceLocations) {
		Arrays.sort(OccurrenceLocations, new Comparator() {
			public int compare(Object arg0, Object arg1) {
				OccurrenceLocation node0= (OccurrenceLocation)arg0;
				OccurrenceLocation node1= (OccurrenceLocation)arg1;
				return node0.getOffset() - node1.getOffset();
			}
		});
	}

	//pattern must be found - otherwise it's assumed to be an error
	private OccurrenceLocation find(StringBuffer s, String pattern, int ithOccurrence) {
		if (ithOccurrence < 1)
			throw new IllegalStateException("ithOccurrence = " + ithOccurrence);
		return find(s, pattern, ithOccurrence, 0);
	}

	private OccurrenceLocation find(StringBuffer s, String pattern, int ithOccurrence, int startIdx) {
		if (startIdx < 0 || startIdx > s.length())
			throw new IllegalStateException("startIdx = " + startIdx);
		int idx= s.indexOf(pattern, startIdx);
		if (idx == -1)
			throw new IllegalStateException("not found \"" + pattern + "\" in \"" + s.substring(startIdx));
		if (ithOccurrence == 1)
			return new OccurrenceLocation(idx, pattern.length(), 0, "");
		return find(s, pattern, ithOccurrence - 1, idx + 1);
	}

	public void testMarkMethodExits1() throws Exception {
		fFinder= new MethodExitsFinder();
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   int foo(String s) throws Exception {\n");
		s.append("      try {\n");
		s.append("         if (s == null)\n");
		s.append("            throw new NullPointerException();\n");
		s.append("         else if (s.length() > 10)\n");
		s.append("            throw new IllegalArgumentException();\n");
		s.append("         else\n");
		s.append("            throw new Exception();\n");
		s.append("      } catch (NullPointerException e) {\n");
		s.append("         e.printStackTrace();\n");
		s.append("      } catch (IllegalArgumentException e) {\n");
		s.append("         e.printStackTrace();\n");
		s.append("      }\n");
		s.append("      return s.length();\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("int");//middle of word
		int length= 0;
		OccurrenceLocation[] ranges= { find(s, "int", 1), find(s, "throw", 4), find(s, "return s.length();", 1) };
		checkSelection(s, offset, length, ranges);
	}

	public void testMarkMethodExits2() throws Exception {
		fFinder= new MethodExitsFinder();
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   int foo(String s) throws Exception {\n");
		s.append("      try {\n");
		s.append("         if (s == null)\n");
		s.append("            throw new NullPointerException();\n");
		s.append("         else if (s.length() > 10)\n");
		s.append("            throw new IllegalArgumentException();\n");
		s.append("         else\n");
		s.append("            throw new Exception();\n");
		s.append("      } catch (NullPointerException | IllegalArgumentException e) {\n");
		s.append("         e.printStackTrace();\n");
		s.append("      }\n");
		s.append("      return s.length();\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("int");//middle of word
		int length= 0;
		OccurrenceLocation[] ranges= { find(s, "int", 1), find(s, "throw", 4), find(s, "return s.length();", 1) };
		checkSelection(s, offset, length, ranges);
	}

	public void testThrowingException1() throws Exception {
		fFinder= new ExceptionOccurrencesFinder();
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   int foo(String s) throws Exception {\n");
		s.append("      try {\n");
		s.append("         if (s == null)\n");
		s.append("            throw new NullPointerException();\n");
		s.append("         else if (s.length() > 10)\n");
		s.append("            throw new IllegalArgumentException();\n");
		s.append("         else\n");
		s.append("            throw new Exception();\n");
		s.append("      } catch (NullPointerException e) {\n");
		s.append("         e.printStackTrace();\n");
		s.append("      } catch (IllegalArgumentException e) {\n");
		s.append("         e.printStackTrace();\n");
		s.append("      }\n");
		s.append("      return s.length();\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("NullPointerException e");//middle of word
		int length= 0;
		OccurrenceLocation[] ranges= { find(s, "throw", 2), find(s, "NullPointerException", 2) };
		checkSelection(s, offset, length, ranges);
	}

	public void testThrowingException2() throws Exception {
		fFinder= new ExceptionOccurrencesFinder();
		StringBuffer s= new StringBuffer();
		s.append("class A{\n");
		s.append("   int foo(String s) throws Exception {\n");
		s.append("      try {\n");
		s.append("         if (s == null)\n");
		s.append("            throw new NullPointerException();\n");
		s.append("         else if (s.length() > 10)\n");
		s.append("            throw new IllegalArgumentException();\n");
		s.append("         else\n");
		s.append("            throw new Exception();\n");
		s.append("      } catch (NullPointerException | IllegalArgumentException e) {\n");
		s.append("         e.printStackTrace();\n");
		s.append("      }\n");
		s.append("      return s.length();\n");
		s.append("   }\n");
		s.append("}\n");
		int offset= 1 + s.indexOf("NullPointerException | ");//middle of word
		int length= 0;
		OccurrenceLocation[] ranges= { find(s, "throw", 2), find(s, "NullPointerException", 2) };
		checkSelection(s, offset, length, ranges);
	}
}
