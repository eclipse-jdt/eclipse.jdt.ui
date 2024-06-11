/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.FastPartitioner;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.JavaHeuristicScanner;
import org.eclipse.jdt.internal.ui.text.JavaIndenter;

/**
 * SmartSemicolonAutoEditStrategyTest
 * @since 3.0
 */
public class JavaHeuristicScannerTest {
	private FastPartitioner fPartitioner;
	private Document fDocument;
	private JavaIndenter fScanner;
	private JavaHeuristicScanner fHeuristicScanner;

	@Before
	public void setUp() {
		if (JavaCore.getPlugin() != null) {
			Hashtable<String, String> options= JavaCore.getDefaultOptions();
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
			options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");

			final String indentOnColumn= DefaultCodeFormatterConstants.createAlignmentValue(false, DefaultCodeFormatterConstants.WRAP_NO_SPLIT, DefaultCodeFormatterConstants.INDENT_ON_COLUMN);
			options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_PARAMETERS_IN_METHOD_DECLARATION, indentOnColumn);
			options.put(DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_EXPRESSIONS_IN_ARRAY_INITIALIZER, indentOnColumn);
			options.put(DefaultCodeFormatterConstants.FORMATTER_CONTINUATION_INDENTATION, "1");
			JavaCore.setOptions(options);
		}

		fDocument= new Document();
		String[] types= new String[] {
			IJavaPartitions.JAVA_DOC,
			IJavaPartitions.JAVA_MULTI_LINE_COMMENT,
			IJavaPartitions.JAVA_SINGLE_LINE_COMMENT,
			IJavaPartitions.JAVA_STRING,
			IJavaPartitions.JAVA_CHARACTER,
			IDocument.DEFAULT_CONTENT_TYPE
		};
		fPartitioner= new FastPartitioner(new FastJavaPartitionScanner(), types);
		fPartitioner.connect(fDocument);
		fDocument.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, fPartitioner);

		fHeuristicScanner= new JavaHeuristicScanner(fDocument);
		fScanner= new JavaIndenter(fDocument, fHeuristicScanner);
	}

	@After
	public void tearDown() throws Exception {
		fDocument.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, null);
		fPartitioner.disconnect();
		fPartitioner= null;
		fDocument= null;

		if (JavaCore.getPlugin() != null) {
			JavaCore.setOptions(JavaCore.getDefaultOptions());
		}
	}

	@Test
	public void testPrevIndentationUnit1() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"");

		int pos= fScanner.findReferencePosition(18);
		assertEquals(9, pos);
	}

	@Test
	public void testPrevIndentationUnit2() {
		fDocument.set("""
				int a;
				if (true)
					return a\
			""");

		int pos= fScanner.findReferencePosition(28);
		assertEquals(21, pos);
	}


	@Test
	public void testPrevIndentationUnit5() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;\n" +
			"");

		int pos= fScanner.findReferencePosition(30);
		assertEquals(9, pos);
	}

	@Test
	public void testPrevIndentationUnit6() {
		// method definition
		fDocument.set("\tvoid proc (int par1, int par2\n");

		int pos= fScanner.findReferencePosition(30);
		assertEquals(12, pos);
	}

	@Test
	public void testPrevIndentationUnit7() {
		// for with semis
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) \n" +
			"");

		int pos= fScanner.findReferencePosition(fDocument.getLength());
		assertEquals(39, pos);
	}

	@Test
	public void testPrevIndentationUnit8() {
		// TODO this is mean - comment at indentation spot
		fDocument.set("\t/* package */ void proc (int par1, int par2) {\n");

		int pos= fScanner.findReferencePosition(fDocument.getLength());
//		assertEquals(1, pos);
		assertEquals(15, pos);
	}

	@Test
	public void testPrevIndentationUnit9() {
		// block
		fDocument.set("""
				void proc (int par1, int par2) {
				\t
					for (int i= 4; i < 33; i++) {
					}
				\t
					int i;
			""");

		int pos= fScanner.findReferencePosition(fDocument.getLength());
		assertEquals(fDocument.getLength() - 7, pos);
	}

	@Test
	public void testPrevIndentationUnit10() {
		// if else
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition()) {\n" +
			"\t\t\tcode();\n" +
			"\t\t} else {\n" +
			"\t\t\totherCode();\n" +
			"\t\t}\n" +
			"");

		int pos= fScanner.findReferencePosition(fDocument.getLength());
		assertEquals(39, pos);
	}

	@Test
	public void testPrevIndentationUnit11() {
		// inside else block
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition()) {\n" +
			"\t\t\tcode();\n" +
			"\t\t} else {\n" +
			"\t\t\totherCode();\n" +
			"\t\t" +
			"");

		int pos= fScanner.findReferencePosition(fDocument.getLength());
		assertEquals(83, pos);
	}

	@Test
	public void testPrevIndentation1() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"");

		String indent= fScanner.getReferenceIndentation(18).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testPrevIndentation2() {
		fDocument.set("""
				int a;
				if (true)
					return a\
			""");

		String indent= fScanner.getReferenceIndentation(28).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testPrevIndentation3() {
		fDocument.set("""
				int a;
				if (true)
					return a;\
			""");

		String indent= fScanner.getReferenceIndentation(29).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testPrevIndentation4() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a\n" +
			"");

		String indent= fScanner.getReferenceIndentation(29).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testPrevIndentation5() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;\n" +
			"");

		String indent= fScanner.getReferenceIndentation(30).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testPrevIndentation6() {
		fDocument.set("\tvoid proc (int par1, int par2\n");

		String indent= fScanner.getReferenceIndentation(30).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testPrevIndentation7() {
		// for with semis
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) \n" +
			"");

		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testPrevIndentation8() {
		fDocument.set("\t/* package */ void proc (int par1, int par2) {\n");

		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testPrevIndentation9() {
		// block
		fDocument.set("""
				void proc (int par1, int par2) {
				\t
					for (int i= 4; i < 33; i++) {
					}
				\t
					int i;
			""");

		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testPrevIndentation10() {
		// else
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition()) {\n" +
			"\t\t\tcode();\n" +
			"\t\t} else {\n" +
			"\t\t\totherCode();\n" +
			"\t\t}\n" +
			"");

		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testPrevIndentation11() {
		// else
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition()) {\n" +
			"\t\t\tcode();\n" +
			"\t\t} else {\n" +
			"\t\t\totherCode();\n" +
			"\t\t" +
			"");

		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testIndentation1() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"");

		String indent= fScanner.computeIndentation(18).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation5() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;\n" +
			"");

		String indent= fScanner.computeIndentation(30).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation6() {
		// parameter declaration - alignment with parenthesis
		fDocument.set("\tvoid proc (int par1, int par2\n");

		String indent= fScanner.computeIndentation(30).toString();
		assertEquals("\t           ", indent);
	}

	@Test
	public void testIndentation6a() {
		// parameter declaration - alignment with parenthesis
		fDocument.set("\tvoid proc (  int par1, int par2\n");

		String indent= fScanner.computeIndentation(30).toString();
		assertEquals("\t             ", indent);
	}

	@Test
	public void testIndentation7() {
		// for with semis
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) \n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testIndentation8() {
		// method definition
		fDocument.set("\t/* package */ void proc (int par1, int par2) {\n");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation9() {
		// block
		fDocument.set("""
				void proc (int par1, int par2) {
				\t
					for (int i= 4; i < 33; i++) {
					}
				\t
					int i;
			""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation10() {
		// else
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition()) {\n" +
			"\t\t\tcode();\n" +
			"\t\t} else {\n" +
			"\t\t\totherCode();\n" +
			"\t\t}\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation11() {
		// else
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition()) {\n" +
			"\t\t\tcode();\n" +
			"\t\t} else {\n" +
			"\t\t\totherCode();\n" +
			"\t\t" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testIndentation12() {
		// multi-line condition
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition1()\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testIndentation13() {
		// multi-line call
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tthis.doStuff(param1, param2,\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testIndentation14() {
		// multi-line array initializer
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tString[] arr= new String[] { a1, a2,\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("		                             ", indent);
	}

	@Test
	public void testIndentation15() {
		// for
		fDocument.set("""
				for (int i= 0; i < 10; i++) {
					bar(); bar(); // foo
				}
			""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation16() {
		// if
		fDocument.set("\tif (true)\n" +
			"\t\t;");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation17() {
		// if
		fDocument.set("\tif (true)\n" +
			";");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation18() {
		// if
		fDocument.set("\tif (true)\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation19() {
		// if w/ brace right after }
		fDocument.set("\tif (true) {\n" +
			"\t\t}");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation20() {
		// if w/ brace right before }
		fDocument.set("\tif (true) {\n" +
			"\t\t}");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation21() {
		// double if w/ brace
		fDocument.set("\tif (true)\n" +
			"\t\tif (true) {\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testIndentation22() {
		// after double if w/ brace
		fDocument.set("\tif (true)\n" +
			"\t\tif (true) {\n" +
			"\t\t\tstuff();" +
			"\t\t}\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t", indent); // because of possible dangling else
	}

	@Test
	public void testIndentation22a() {
		// after double if w/ brace
		fDocument.set("\tif (true)\n" +
			"\t\tif (true) {\n" +
			"\t\t\tstuff();\n" +
			"\t\t}\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 2).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation22b() {
		// after double if w/ brace
		fDocument.set("""
				if (true)
					if (true) {
						stuff();\
					}
			a""");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		assertEquals("\t", indent); // no dangling else possible
	}

	@Test
	public void testIndentation23() {
		// do
		fDocument.set("\tdo\n" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation24() {
		// braceless else
		fDocument.set("""
				if (true) {
					stuff();
				} else
					noStuff\
			""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation25() {
		// braceless else
		fDocument.set("""
				if (true) {\r
					stuff();\r
				} else\r
					noStuff;\r
			""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation26() {
		// do while
		fDocument.set("\tdo\n" +
			"\t\t\n" +
			"\twhile (true);" +
			"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation27() {
		// do while
		fDocument.set("\tdo\n" +
			"\t\t;\n" +
			"\twhile (true);" +
			"");

		int i= fScanner.findReferencePosition(8);
		assertEquals(1, i);
		String indent= fScanner.computeIndentation(8).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation28() {
		// TODO do while - how to we distinguish from while {} loop?
		fDocument.set("\tdo\n" +
			"\t\t;\n" +
			"\twhile (true);" +
			"");

		int i= fScanner.findReferencePosition(fDocument.getLength());
		assertEquals(1, i);
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation29() {
		fDocument.set("""
					while (condition)
						while (condition)
							foo();
			""");

		int i= fScanner.findReferencePosition(fDocument.getLength());
		assertEquals(2, i);
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation30() {
		// braceless else
		fDocument.set("\tif (true)\n" +
			"\t{");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentation31() {
		// braceless else
		fDocument.set("""
				if (true)
			{\t
					stuff();
				} else
					noStuff""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testIndentation32() {
		// braceless else
		fDocument.set("\tswitch(ch) {\n" +
			"\t\tcase one:\n");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("			", indent);
	}

	@Test
	public void testAnonymousIndentation1() {
		fDocument.set(	"""
					MenuItem mi= new MenuItem("About...");
					mi.addActionListener(
						new ActionListener() {
			"""
						);

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("				", indent);
	}

	@Test
	public void testAnonymousIndentation2() {
		fDocument.set(	"""
					MenuItem mi= new MenuItem("About...");
					mi.addActionListener(
						new ActionListener() {
							public void actionPerformed(ActionEvent event) {
								about();
							}
						}
			);"""
						);

		// this is bogus, since this is really just an unfinished call argument list - how could we know
		String indent= fScanner.computeIndentation(fDocument.getLength() - 2).toString();
		assertEquals("			", indent);
	}

	@Test
	public void testExceptionIndentation1() {
		fDocument.set("""
			public void processChildren(CompositeExpression result, IConfigurationElement element) throws CoreException {
						IConfigurationElement[] children= element.getChildren();
						if (children != null) {
							for (int i= 0; i < children.length; i++) {
								Expression child= parse(children[i]);
								if (child == null)
									new Bla(new CoreExeption(new Status(IStatus.ERROR, JavaPlugin.getPluginId()""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("							", indent);
	}

	@Test
	public void testExceptionIndentation2() {
		fDocument.set("""
			public void processChildren(CompositeExpression result, IConfigurationElement element) throws CoreException {
						IConfigurationElement[] children= element.getChildren();
						if (children != null) {
							for (int i= 0; i < children.length; i++) {
								Expression child= parse(children[i]);
								if (child == null)
									new Bla(new CoreExeption(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("							", indent);
	}

	@Test
	public void testExceptionIndentation3() {
		fDocument.set("""
			public void processChildren(CompositeExpression result, IConfigurationElement element) throws CoreException {
						IConfigurationElement[] children= element.getChildren();
						if (children != null) {
							for (int i= 0; i < children.length; i++) {
								Expression child= parse(children[i]);
								if (child == null)
									new char[] { new CoreExeption(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),""");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("							", indent);
	}

	@Test
	public void testListAlignmentMethodDeclaration() {
		// parameter declaration - alignment with parenthesis
		fDocument.set(	"\tvoid proc (  int par1, int par2,\n" +
				"	   int par3, int par4,\n");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("	   ", indent);
	}

	@Test
	public void testListAlignmentMethodCall() {
		// parameter declaration - alignment with parenthesis
		fDocument.set(	"\this.proc (par1, par2,\n" +
				"	   par3, par4,\n");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("	   ", indent);
	}

	@Test
	public void testListAlignmentArray() {
		// parameter declaration - alignment with parenthesis
		fDocument.set(	"\tint[]= new int[] { 1, two,\n" +
				"	   three, four,\n");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("	   ", indent);
	}

	@Test
	public void testListAlignmentArray2() {
		// no prior art - probe system settings.
		fDocument.set(	"\tint[]= new int[] { 1, two,\n");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("\t                   ", indent);

	}

	@Test
	public void testBraceAlignmentOfMultilineDeclaration() {
		fDocument.set(	"""
				protected int foobar(int one, int two,
									 int three, int four,
									 int five) {
				\t
					return 0;
				}\
			""");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		assertEquals("	", indent);
	}

	@Test
	public void testBlocksInCaseStatements() {
		fDocument.set(
				"		switch (i) {\n" +
				"			case 1:\n" +
				"				new Runnable() {\n" +
				"");

		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		assertEquals("					", indent);
	}

	@Test
	public void testAnonymousTypeBraceNextLine() throws Exception {
		fDocument.set(
				"""
							MenuItem mi= new MenuItem("About...");
							mi.addActionListener(new ActionListener() \
							{
					"""
				);

		String indent= fScanner.computeIndentation(fDocument.getLength() - 2).toString();
		assertEquals("		", indent);

    }

	/*
	 * @since 3.2
	 */
	@Test
	public void testClassInstanceCreationHeuristic() throws Exception {
	    fDocument.set("   method(new java.util.ArrayList<String>(10), foo, new int[])");

	    for (int offset= 0; offset < 15; offset++)
	    	assertFalse(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 15; offset < 19; offset++)
	    	assertTrue(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 19; offset < 20; offset++)
	    	assertFalse(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 20; offset < 24; offset++)
	    	assertTrue(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 24; offset < 25; offset++)
	    	assertFalse(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 25; offset < 34; offset++)
	    	assertTrue(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 34; offset < 57; offset++)
	    	assertFalse(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 57; offset < 60; offset++)
	    	assertTrue(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
	    for (int offset= 60; offset < 63; offset++)
	    	assertFalse(fHeuristicScanner.looksLikeClassInstanceCreationBackward(offset, JavaHeuristicScanner.UNBOUND));
    }

	@Ignore("enable when https://bugs.eclipse.org/bugs/show_bug.cgi?id=65463 is fixed")
	@Test
	public void testConditional1() throws Exception {
    	fDocument.set(
    			"""
							public boolean isPrime() {
								return fPrime == true ? true
								                      : false;\
					"""
    	);

    	String indent= fScanner.computeIndentation(fDocument.getLength() - 8).toString();
    	assertEquals("			                      ", indent);
    }

	@Ignore("enable when https://bugs.eclipse.org/bugs/show_bug.cgi?id=65463 is fixed")
	@Test
	public void testConditional2() throws Exception {
    	fDocument.set(
    			"""
							public boolean isPrime() {
								return fPrime == true\
										? true
										: false;\
					"""
    	);

    	String indent= fScanner.computeIndentation(fDocument.getLength() - 8).toString();
    	assertEquals("					", indent);

    }

	@Test
	public void testTernaryAfterConditional() throws Exception {
		fDocument.set(
		"		if (true)\n" +
		"			ret = true ? \"\".substring(0) : \"\";\n");

		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		assertEquals("		", indent);
	}

	@Test
	public void testContinuationIndentationOfForStatement() throws Exception {
		fDocument.set("""
				for (int i = (2 * 2); i < array.length; i++) {
				int i= 25;
				}\
			""");

		String indent= fScanner.computeIndentation(22).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(27).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(39).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(40).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(5).toString();
		assertEquals("\t", indent);
		indent= fScanner.computeIndentation(45).toString();
		assertEquals("\t", indent);
		indent= fScanner.computeIndentation(60).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testContinuationIndentationOfForEachStatement() throws Exception {
		// Bug 331028 and Bug 331734
		fDocument.set("""
				for (int value : values) {
					sum += value;
							System.out.println(sum);
				}\
			""");

		String indent= fScanner.computeIndentation(44).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfForEachStatement2() throws Exception {
		// Bug 348198
		fDocument.set("""
				for (int value : values)
					sum += value;
							System.out.println(sum);
				}\
			""");

		String indent= fScanner.computeIndentation(44).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testContinuationIndentationOfBooleanExpression() throws Exception {
		fDocument.set("\tboolean a = true || false;\n" +
				"\tboolean b = a || false;\n");

		String indent= fScanner.computeIndentation(20).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(40).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfReturnStatement() throws Exception {
		fDocument.set("\t\treturn \"I'm such a long string that you have to split me to see the whole line without scrolling around\"\n");

		String indent= fScanner.computeIndentation(8).toString();
		assertEquals("\t\t\t", indent);
		indent= fScanner.computeIndentation(21).toString();
		assertEquals("\t\t\t", indent);
		indent= fScanner.computeIndentation(38).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfAssignmentStatement() throws Exception {
		fDocument.set("\tint i= 5+");

		String indent= fScanner.computeIndentation(7).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(10).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfThrowsClause() throws Exception {
		fDocument.set("\tprivate void thrower() throws java.sql.SQLException, java.io.IOException {");

		String indent= fScanner.computeIndentation(23).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(24).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfParentheses() throws Exception {
		fDocument.set("\tint foo() {\n\treturn \"\".length(\n\t\t);\n\t}");

		String indent= fScanner.computeIndentation(34).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfAnnotation() throws Exception {
		fDocument.set("\t@MyAnnotation(\n\t\tvalue=\"hello\")\n\t\tpublic class ArrayAnnotationBug {\n\t\t}");
		String indent= fScanner.computeIndentation(33).toString();
		assertEquals("\t", indent);

		fDocument.set("\t@org.eclipse.jdt.MyAnnotation(\n\t\tvalue=\"hello\")\n\t\tpublic class ArrayAnnotationBug {\n\t\t}");
		indent= fScanner.computeIndentation(49).toString();
		assertEquals("\t", indent);
	}

	@Test
	public void testIndentationAfterIfTryCatch() throws Exception {
		fDocument.set("""
				public class Bug237081 {
					public void foo() {
						if (true)
							try {
							} catch (RuntimeException ex) {
							}
							foo();
					}
				}\
			""");
		String indent= fScanner.computeIndentation(117).toString();
		assertEquals("\t\t\t", indent);

		fDocument.set("""
				public class Bug237081 {
					public void foo() {
						if (true)
							try {
							} catch (RuntimeException ex) {
							} catch (RuntimeException ex) {
							} finally {
							}
					foo();
					}
				}\
			""");
		indent= fScanner.computeIndentation(167).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfBrackets() throws Exception {
		fDocument.set("\tprivate void helper2(boolean[] booleans) {\n\t}");

		String indent= fScanner.computeIndentation(31).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(30).toString();
		assertEquals("\t                             ", indent);

		fDocument.set("\tif (booleans[0]) {\n\t\tString[] aString= new String[]{\"a\", \"b\"};\n\t\tbooleans[5]= true;\n\t}");
		indent= fScanner.computeIndentation(16).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(14).toString();
		assertEquals("\t             ", indent);
		indent= fScanner.computeIndentation(30).toString();
		assertEquals("\t\t\t", indent);
		indent= fScanner.computeIndentation(52).toString();
		assertEquals("\t\t\t", indent);
		indent= fScanner.computeIndentation(77).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfStrings1() throws Exception {
		fDocument.set(
				"	String[] i = new String[] {\n" + //0-28
				"		\"X.java\",\n" + //29-40
				"		\"public class X extends B{\"\n" + //41-70
				"		+ \"test\"\n" +	//71-81
				"		+ \"    public \"};");//82-

		String indent= fScanner.computeIndentation(73).toString(); // at the beginning of 4th line
		assertEquals("\t\t\t", indent);
		indent= fScanner.computeIndentation(84).toString(); // at the beginning of 5th line
		assertEquals("\t\t", indent);

		fDocument.set(
				"	String[] i = new String[] {\n" +//0-28
				"		\"X.java\",\n" + //29-40
				"		\"public class X extends B{\" +\n" + //41-72
				"		\"test\" +\n" + //73-
				"		\"    public\"\n};");

		indent= fScanner.computeIndentation(75).toString(); //at the beginning of 4th line
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfStrings2() throws Exception {
		//Bug 338229
		fDocument.set(
				"	System.out.println(\"Some\"\n" + //0-26
				"		+ new Object()\n" + //27-43
						"		+ \"string:\\n\" + definedType.toString());\n"); //44-

		String indent= fScanner.computeIndentation(59).toString(); //before the last +
		assertEquals("\t\t", indent);
	}

	@Test
	public void testContinuationIndentationOfStrings3() throws Exception {
		fDocument.set(
				"	String test =\n" + //0-14
				"		\"this is the 1st string\"\n" + //15-41
				"		+ \"this is the 1st string\";\n");//42-

		String indent= fScanner.computeIndentation(44).toString();//at the beginning of 3rd line
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testContinuationIndentation1() throws Exception {
		fDocument.set("\treturn (thisIsAVeryLongName == 1 && anotherVeryLongName == 1)\n" +
				"\t\t|| thisIsAVeryLongName == 2;");

		String indent= fScanner.computeIndentation(68).toString();
		assertEquals("\t\t", indent);
		indent= fScanner.computeIndentation(88).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testContinuationIndentation2() {
		fDocument.set("\tint a;\n" +
				"\tif (true)\n" +
				"\t\treturn a\n" +
				"");

		int pos= fScanner.findReferencePosition(29);
		assertEquals(21, pos);
	}

	@Test
	public void testContinuationIndentation3() {
		fDocument.set("""
				int a;
				if (true)
					return a\
			""");

		String indent= fScanner.computeIndentation(28).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testContinuationIndentation4() {
		fDocument.set("""
				int a;
				if (true)
					return a;\
			""");

		String indent= fScanner.computeIndentation(29).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testContinuationIndentation5() {
		fDocument.set("\tint a;\n" +
				"\tif (true)\n" +
				"\t\treturn a\n" +
				"");

		String indent= fScanner.computeIndentation(29).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testIndentationTryWithResources() throws Exception {
		String s= """
			class A {
				void foo() throws Throwable {
					try (FileReader reader1 = new FileReader("file1");
						FileReader reader2 = new FileReader("file2");
						FileReader reader3 = new FileReader("file3");
						FileReader reader4 = new FileReader("file4");
						FileReader reader5 = new FileReader("file5")) {
						int ch;
						while ((ch = reader1.read()) != -1) {
							System.out.println(ch);
						}
					}
				}
			""";

		fDocument.set(s);

		int offset= s.indexOf("FileReader reader2");
		String indent= fScanner.computeIndentation(offset).toString();
		assertEquals("\t\t\t", indent);

		offset= s.indexOf("FileReader reader5");
		indent= fScanner.computeIndentation(offset).toString();
		assertEquals("\t\t\t", indent);
	}

	@Test
	public void testDefaultMethod1() {
		StringBuilder buf= new StringBuilder();
		buf.append("interface I {\n");
		buf.append("			default void foo (int a) {\n");
		buf.append("		switch(a) {\n");
		buf.append("		case 0: \n");
		buf.append("			break;\n");
		buf.append("	default : \n");
		buf.append("			System.out.println(\"default\");\n");
		buf.append("		}\n");
		buf.append("	}\n");
		buf.append("}\n");
		fDocument.set(buf.toString());

		int offset= buf.indexOf("default void");
		String indent= fScanner.computeIndentation(offset).toString();
		assertEquals("\t", indent);

		offset= buf.indexOf("default :");
		indent= fScanner.computeIndentation(offset).toString();
		assertEquals("\t\t", indent);
	}

	@Test
	public void testDefaultMethod2() {
		StringBuilder buf= new StringBuilder();
		buf.append("interface I {\n");
		buf.append("    default String name() {\n");
		buf.append("        return \"unnamed\";\n");
		buf.append("    }\n");
		buf.append("}\n");
		fDocument.set(buf.toString());

		int offset= buf.indexOf("default String");
		String indent= fScanner.computeIndentation(offset).toString();
		assertEquals("\t", indent);
	}
}
