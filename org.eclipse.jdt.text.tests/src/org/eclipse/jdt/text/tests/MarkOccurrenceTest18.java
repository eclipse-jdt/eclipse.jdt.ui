/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.testplugin.JavaProjectHelper;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.tests.core.Java18ProjectTestSetup;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.corext.dom.IASTSharedValues;
import org.eclipse.jdt.internal.ui.search.ExceptionOccurrencesFinder;
import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder;
import org.eclipse.jdt.internal.core.manipulation.search.IOccurrencesFinder.OccurrenceLocation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MarkOccurrenceTest18 extends TestCase {
	private static final Class<MarkOccurrenceTest18> THIS= MarkOccurrenceTest18.class;

	public static Test suite() {
		return new Java18ProjectTestSetup(new TestSuite(THIS));
	}

	public static Test setUpTest(Test test) {
		return new Java18ProjectTestSetup(test);
	}

	private ASTParser fParser;

	private IOccurrencesFinder fFinder;

	private IJavaProject fJProject1;

	private IPackageFragmentRoot fSourceFolder;

	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		fParser= ASTParser.newParser(IASTSharedValues.SHARED_AST_LEVEL);

		fJProject1= Java18ProjectTestSetup.getProject();
		fSourceFolder= JavaProjectHelper.addSourceContainer(fJProject1, "src");
		JavaPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.EDITOR_MARK_OCCURRENCES, true);
	}

	@Override
	protected void tearDown() throws Exception {
		JavaProjectHelper.clear(fJProject1, Java18ProjectTestSetup.getDefaultClasspath());
	}

	private OccurrenceLocation[] getHighlights(StringBuffer source, int offset, int length) throws Exception {
		CompilationUnit root= createCompilationUnit(source);
		String errorString= fFinder.initialize(root, offset, length);
		assertNull(errorString, errorString);
		return fFinder.getOccurrences();
	}

	private CompilationUnit createCompilationUnit(StringBuffer source) throws JavaModelException {
		IPackageFragment pack1= fSourceFolder.createPackageFragment("test1", false, null);
		ICompilationUnit cu= pack1.createCompilationUnit("E.java", source.toString(), true, null);
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
		Arrays.sort(OccurrenceLocations, new Comparator<OccurrenceLocation>() {
			@Override
			public int compare(OccurrenceLocation node0, OccurrenceLocation node1) {
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

	public void testThrowingAnnotatedException1() throws Exception {
		StringBuffer s= new StringBuffer();
		s.append("package test1;\n");
		s.append("import java.lang.annotation.Documented;\n");
		s.append("import java.lang.annotation.ElementType;\n");
		s.append("import java.lang.annotation.Retention;\n");
		s.append("import java.lang.annotation.RetentionPolicy;\n");
		s.append("import java.lang.annotation.Target;\n");
		s.append("\n");
		s.append("public class E {\n");
		s.append("    @Target(ElementType.TYPE_USE)\n");
		s.append("    @Retention(RetentionPolicy.RUNTIME)\n");
		s.append("    @Documented\n");
		s.append("    static @interface Critical {\n");
		s.append("        String msg() default \"We're all going to die!\";\n");
		s.append("    }\n");
		s.append("\n");
		s.append("    class InnerException extends Exception {\n");
		s.append("        private static final long serialVersionUID = 1L;\n");
		s.append("    }\n");
		s.append("\n");
		s.append("    /**\n");
		s.append("     * @throws E.InnerException\n");
		s.append("     */\n");
		s.append("    void foo() throws @Critical() E.@Critical() InnerException, IllegalArgumentException {\n");
		s.append("        if (Boolean.TRUE)\n");
		s.append("            throw new @Critical() InnerException();\n");
		s.append("        else\n");
		s.append("            throw new @Critical() E.@Critical() InnerException();\n");
		s.append("    }\n");
		s.append("\n");
		s.append("    void tryCatch() {\n");
		s.append("        try {\n");
		s.append("            foo();\n");
		s.append("        } catch (@Critical() E.@Critical() InnerException e) {\n");
		s.append("        } catch (RuntimeException e) {\n");
		s.append("        }\n");
		s.append("        try {\n");
		s.append("            foo();\n");
		s.append("        } catch (RuntimeException | @Critical(msg=\"He\"+\"llo\") E.@Critical() InnerException e) {\n");
		s.append("        }\n");
		s.append("    }\n");
		s.append("}\n");
		
		fFinder= new ExceptionOccurrencesFinder();
		int offset= 8 + s.indexOf("@throws E.InnerException"); // in Javadoc
		int length= 0;
		OccurrenceLocation[] ranges= { find(s, "E.InnerException", 1), find(s, "@Critical() E.@Critical() InnerException", 1),
				find(s, "throw", 3), find(s, "throw", 4) };
		checkSelection(s, offset, length, ranges);
		
		fFinder= new ExceptionOccurrencesFinder();
		offset= 1 + s.indexOf("@Critical() E.@Critical() InnerException"); // in annotation
		length= 0;
		checkSelection(s, offset, length, ranges);
		
		fFinder= new ExceptionOccurrencesFinder();
		offset= s.indexOf("E.@Critical() InnerException"); // in annotated type qualifier
		length= 1;
		checkSelection(s, offset, length, ranges);
		
		fFinder= new ExceptionOccurrencesFinder();
		offset= 1 + s.indexOf("InnerException e)"); // in annotated catch type (does NOT include modifier "@Critical() "!)
		length= 0;
		ranges= new OccurrenceLocation[] { find(s, "foo", 2), find(s, "E.@Critical() InnerException", 3)};
		checkSelection(s, offset, length, ranges);
		
		fFinder= new ExceptionOccurrencesFinder();
		offset= s.indexOf("RuntimeException |"); // in annotated union type
		length= 0;
		ranges= new OccurrenceLocation[] { find(s, "foo", 3), find(s, "RuntimeException", 2)};
		checkSelection(s, offset, length, ranges);
		
		fFinder= new ExceptionOccurrencesFinder();
		offset= s.indexOf("He"); // in annotation expression
		length= 0;
		ranges= new OccurrenceLocation[] { find(s, "foo", 3), find(s, "@Critical(msg=\"He\"+\"llo\") E.@Critical() InnerException", 1)};
		checkSelection(s, offset, length, ranges);
	}

	public void testThrownExceptionInLambda() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.IOException;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    void test(int i) throws IOException {\n");
		buf.append("        if (i == 0) {\n");
		buf.append("            throw new IOException();\n");
		buf.append("        } else {\n");
		buf.append("            FI fi = () -> { throw new IOException(); };\n");
		buf.append("        }\n");
		buf.append("    }\n");
		buf.append("}\n");
		buf.append("\n");
		buf.append("@FunctionalInterface\n");
		buf.append("interface FI {\n");
		buf.append("    void foo() throws IOException;\n");
		buf.append("}\n");

		fFinder= new ExceptionOccurrencesFinder();
		int offset= buf.indexOf("IOException {");
		int length= 0;
		OccurrenceLocation[] ranges= { find(buf, "IOException", 2), find(buf, "throw", 2) };
		checkSelection(buf, offset, length, ranges);
	}

	public void testTryCatchInLambda() throws Exception {
		StringBuffer buf= new StringBuffer();
		buf.append("package test1;\n");
		buf.append("\n");
		buf.append("import java.io.FileInputStream;\n");
		buf.append("import java.io.FileNotFoundException;\n");
		buf.append("\n");
		buf.append("public class E {\n");
		buf.append("    Runnable lambda = () -> {\n");
		buf.append("        try {\n");
		buf.append("            new FileInputStream(\"dummy\");\n");
		buf.append("        } catch (FileNotFoundException e) {\n");
		buf.append("        }\n");
		buf.append("    };\n");
		buf.append("}\n");

		fFinder= new ExceptionOccurrencesFinder();
		int offset= buf.indexOf("FileNotFoundException e");
		int length= 0;
		OccurrenceLocation[] ranges= { find(buf, "FileInputStream", 2), find(buf, "FileNotFoundException", 2) };
		checkSelection(buf, offset, length, ranges);
	}

}
