/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import java.util.Hashtable;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
public class JavaHeuristicScannerTest extends TestCase {

	private FastPartitioner fPartitioner;
	private Document fDocument;
	private JavaIndenter fScanner;
	private JavaHeuristicScanner fHeuristicScanner;

	public static Test suite() {
		return new TestSuite(JavaHeuristicScannerTest.class);
	}

	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() {
		if (JavaCore.getPlugin() != null) {
			Hashtable options= JavaCore.getDefaultOptions();
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
	
	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		fDocument.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, null);
		fPartitioner.disconnect();
		fPartitioner= null;
		fDocument= null;

		if (JavaCore.getPlugin() != null) {
			JavaCore.setOptions(JavaCore.getDefaultOptions());
		}
}

	public void testPrevIndentationUnit1() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"");
		
		int pos= fScanner.findReferencePosition(18);
		Assert.assertEquals(9, pos);
	}
	
	public void testPrevIndentationUnit2() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a");
		
		int pos= fScanner.findReferencePosition(28);
		Assert.assertEquals(21, pos);
	}
	
	public void testPrevIndentationUnit4() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a\n" +
			"");
		
		int pos= fScanner.findReferencePosition(29);
		Assert.assertEquals(28, pos);
	}
	
	public void testPrevIndentationUnit5() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;\n" +
			"");
		
		int pos= fScanner.findReferencePosition(30);
		Assert.assertEquals(9, pos);
	}
	
	public void testPrevIndentationUnit6() {
		// method definition
		fDocument.set("\tvoid proc (int par1, int par2\n");
		
		int pos= fScanner.findReferencePosition(30);
		Assert.assertEquals(12, pos);
	}
	
	public void testPrevIndentationUnit7() {
		// for with semis 
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) \n" +
			"");
		
		int pos= fScanner.findReferencePosition(fDocument.getLength());
		Assert.assertEquals(39, pos);
	}
	
	public void testPrevIndentationUnit8() {
		// TODO this is mean - comment at indentation spot
		fDocument.set("\t/* package */ void proc (int par1, int par2) {\n");
		
		int pos= fScanner.findReferencePosition(fDocument.getLength());
//		Assert.assertEquals(1, pos);
		Assert.assertEquals(15, pos);
	}
	
	public void testPrevIndentationUnit9() {
		// block
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) {\n" +
			"\t\t}\n" +
			"\t\t\n" +
			"\t\tint i;\n");
		
		int pos= fScanner.findReferencePosition(fDocument.getLength());
		Assert.assertEquals(fDocument.getLength() - 7, pos);
	}

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
		Assert.assertEquals(39, pos);
	}

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
		Assert.assertEquals(83, pos);
	}

	public void testPrevIndentation1() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"");
		
		String indent= fScanner.getReferenceIndentation(18).toString();
		Assert.assertEquals("\t", indent);
	}
	
	public void testPrevIndentation2() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a");
		
		String indent= fScanner.getReferenceIndentation(28).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testPrevIndentation3() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;");
		
		String indent= fScanner.getReferenceIndentation(29).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testPrevIndentation4() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a\n" +
			"");
		
		String indent= fScanner.getReferenceIndentation(29).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testPrevIndentation5() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;\n" +
			"");
		
		String indent= fScanner.getReferenceIndentation(30).toString();
		Assert.assertEquals("\t", indent);
	}
	
	public void testPrevIndentation6() {
		fDocument.set("\tvoid proc (int par1, int par2\n");
		
		String indent= fScanner.getReferenceIndentation(30).toString();
		Assert.assertEquals("\t", indent);
	}
	
	public void testPrevIndentation7() {
		// for with semis 
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) \n" +
			"");
		
		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testPrevIndentation8() {
		fDocument.set("\t/* package */ void proc (int par1, int par2) {\n");
		
		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t", indent);
	}
	
	public void testPrevIndentation9() {
		// block
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) {\n" +
			"\t\t}\n" +
			"\t\t\n" +
			"\t\tint i;\n");
		
		String indent= fScanner.getReferenceIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}

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
		Assert.assertEquals("\t\t", indent);
	}

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
		Assert.assertEquals("\t\t\t", indent);
	}

	public void testIndentation1() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"");
		
		String indent= fScanner.computeIndentation(18).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testIndentation2() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a");
		
		String indent= fScanner.computeIndentation(28).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testIndentation3() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;");
		
		String indent= fScanner.computeIndentation(29).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testIndentation4() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a\n" +
			"");
		
		String indent= fScanner.computeIndentation(29).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testIndentation5() {
		fDocument.set("\tint a;\n" +
			"\tif (true)\n" +
			"\t\treturn a;\n" +
			"");
		
		String indent= fScanner.computeIndentation(30).toString();
		Assert.assertEquals("\t", indent);
	}
	
	public void testIndentation6() {
		// parameter declaration - alignment with parenthesis 
		fDocument.set("\tvoid proc (int par1, int par2\n");
		
		String indent= fScanner.computeIndentation(30).toString();
		Assert.assertEquals("\t           ", indent);
	}
	
	public void testIndentation6a() {
		// parameter declaration - alignment with parenthesis 
		fDocument.set("\tvoid proc (  int par1, int par2\n");
		
		String indent= fScanner.computeIndentation(30).toString();
		Assert.assertEquals("\t             ", indent);
	}
	
	public void testIndentation7() {
		// for with semis 
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) \n" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t\t", indent);
	}
	
	public void testIndentation8() {
		// method definition
		fDocument.set("\t/* package */ void proc (int par1, int par2) {\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}
	
	public void testIndentation9() {
		// block
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tfor (int i= 4; i < 33; i++) {\n" +
			"\t\t}\n" +
			"\t\t\n" +
			"\t\tint i;\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}

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
		Assert.assertEquals("\t\t", indent);
	}

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
		Assert.assertEquals("\t\t\t", indent);
	}

	public void testIndentation12() {
		// multi-line condition
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tif (condition1()\n" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t\t", indent);
	}

	public void testIndentation13() {
		// multi-line call
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tthis.doStuff(param1, param2,\n" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t\t", indent);
	}

	public void testIndentation14() {
		// multi-line array initializer
		fDocument.set("\tvoid proc (int par1, int par2) {\n" +
			"\t\t\n" +
			"\t\tString[] arr= new String[] { a1, a2,\n" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("		                             ", indent);
	}

	public void testIndentation15() {
		// for
		fDocument.set("\tfor (int i= 0; i < 10; i++) {\n" +
			"\t\tbar(); bar(); // foo\n" +
			"\t}\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation16() {
		// if
		fDocument.set("\tif (true)\n" +
			"\t\t;");
		
		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation17() {
		// if
		fDocument.set("\tif (true)\n" +
			";");
		
		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation18() {
		// if
		fDocument.set("\tif (true)\n" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation19() {
		// if w/ brace right after }
		fDocument.set("\tif (true) {\n" +
			"\t\t}");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation20() {
		// if w/ brace right before }
		fDocument.set("\tif (true) {\n" +
			"\t\t}");
		
		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation21() {
		// double if w/ brace
		fDocument.set("\tif (true)\n" +
			"\t\tif (true) {\n" +
			"");	
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t\t", indent);
	}

	public void testIndentation22() {
		// after double if w/ brace 
		fDocument.set("\tif (true)\n" +
			"\t\tif (true) {\n" +
			"\t\t\tstuff();" +
			"\t\t}\n" +
			"");	
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t", indent); // because of possible dangling else
	}

	public void testIndentation22a() {
		// after double if w/ brace 
		fDocument.set("\tif (true)\n" +
			"\t\tif (true) {\n" +
			"\t\t\tstuff();\n" +
			"\t\t}\n" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength() - 2).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation22b() {
		// after double if w/ brace 
		fDocument.set("\tif (true)\n" +
			"\t\tif (true) {\n" +
			"\t\t\tstuff();" +
			"\t\t}\n" +
			"a");	
		
		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		Assert.assertEquals("\t", indent); // no dangling else possible
	}

	public void testIndentation23() {
		// do
		fDocument.set("\tdo\n" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation24() {
		// braceless else 
		fDocument.set("\tif (true) {\n" +
			"\t\tstuff();\n" +
			"\t} else\n" +
			"\t\tnoStuff");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation25() {
		// braceless else 
		fDocument.set("\tif (true) {\r\n" +
			"\t\tstuff();\r\n" +
			"\t} else\r\n" +
			"\t\tnoStuff;\r\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation26() {
		// do while
		fDocument.set("\tdo\n" +
			"\t\t\n" +
			"\twhile (true);" +
			"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation27() {
		// do while
		fDocument.set("\tdo\n" +
			"\t\t;\n" +
			"\twhile (true);" +
			"");
		
		int i= fScanner.findReferencePosition(8);
		Assert.assertEquals(1, i);
		String indent= fScanner.computeIndentation(8).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation28() {
		// TODO do while - how to we distinguish from while {} loop?
		fDocument.set("\tdo\n" +
			"\t\t;\n" +
			"\twhile (true);" +
			"");
		
		int i= fScanner.findReferencePosition(fDocument.getLength());
		Assert.assertEquals(1, i);
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation29() {
		fDocument.set("\t\twhile (condition)\n" +
				"\t\t\twhile (condition)\n" + 
				"\t\t\t\tfoo();\n");
		
		int i= fScanner.findReferencePosition(fDocument.getLength());
		Assert.assertEquals(2, i);
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation30() {
		// braceless else 
		fDocument.set("\tif (true)\n" +
			"\t{");
		
		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		Assert.assertEquals("\t", indent);
	}

	public void testIndentation31() {
		// braceless else 
		fDocument.set("\tif (true)\n" +
			"{\t\n" +
			"\t\tstuff();\n" +
			"\t} else\n" +
			"\t\tnoStuff");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t", indent);
	}

	public void testIndentation32() {
		// braceless else 
		fDocument.set("\tswitch(ch) {\n" +
			"\t\tcase one:\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("			", indent);
	}

	public void testAnonymousIndentation1() {
		fDocument.set(	"		MenuItem mi= new MenuItem(\"About...\");\n" + 
						"		mi.addActionListener(\n" + 
						"			new ActionListener() {\n" 
						);
				
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("				", indent);
	}
	
	public void testAnonymousIndentation2() {
		fDocument.set(	"		MenuItem mi= new MenuItem(\"About...\");\n" + 
						"		mi.addActionListener(\n" + 
						"			new ActionListener() {\n" + 
						"				public void actionPerformed(ActionEvent event) {\n" + 
						"					about();\n" + 
						"				}\n" + 
						"			}\n" +
						");"
						);
				
		// this is bogus, since this is really just an unfinished call argument list - how could we know 
		String indent= fScanner.computeIndentation(fDocument.getLength() - 2).toString();
		Assert.assertEquals("		", indent);
	}
	
	public void testExceptionIndentation1() {
		fDocument.set("public void processChildren(CompositeExpression result, IConfigurationElement element) throws CoreException {\n" + 
				"			IConfigurationElement[] children= element.getChildren();\n" + 
				"			if (children != null) {\n" + 
				"				for (int i= 0; i < children.length; i++) {\n" + 
				"					Expression child= parse(children[i]);\n" + 
				"					if (child == null)\n" + 
				"						new Bla(new CoreExeption(new Status(IStatus.ERROR, JavaPlugin.getPluginId()");
	
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("							", indent);
	}
	
	public void testExceptionIndentation2() {
		fDocument.set("public void processChildren(CompositeExpression result, IConfigurationElement element) throws CoreException {\n" + 
				"			IConfigurationElement[] children= element.getChildren();\n" + 
				"			if (children != null) {\n" + 
				"				for (int i= 0; i < children.length; i++) {\n" + 
				"					Expression child= parse(children[i]);\n" + 
				"					if (child == null)\n" + 
				"						new Bla(new CoreExeption(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("							", indent);
	}
	
	public void testExceptionIndentation3() {
		fDocument.set("public void processChildren(CompositeExpression result, IConfigurationElement element) throws CoreException {\n" + 
				"			IConfigurationElement[] children= element.getChildren();\n" + 
				"			if (children != null) {\n" + 
				"				for (int i= 0; i < children.length; i++) {\n" + 
				"					Expression child= parse(children[i]);\n" + 
				"					if (child == null)\n" + 
				"						new char[] { new CoreExeption(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("							", indent);
	}
	
	public void testListAlignmentMethodDeclaration() {
		// parameter declaration - alignment with parenthesis 
		fDocument.set(	"\tvoid proc (  int par1, int par2,\n" +
				"	   int par3, int par4,\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("	   ", indent);
	}
	
	public void testListAlignmentMethodCall() {
		// parameter declaration - alignment with parenthesis 
		fDocument.set(	"\this.proc (par1, par2,\n" +
				"	   par3, par4,\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("	   ", indent);
	}
	
	public void testListAlignmentArray() {
		// parameter declaration - alignment with parenthesis 
		fDocument.set(	"\tint[]= new int[] { 1, two,\n" +
				"	   three, four,\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("	   ", indent);
	}
	
	public void testListAlignmentArray2() {
		// no prior art - probe system settings. 
		fDocument.set(	"\tint[]= new int[] { 1, two,\n");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t                   ", indent);
		
	}
	
	public void testBraceAlignmentOfMultilineDeclaration() {
		fDocument.set(	"	protected int foobar(int one, int two,\n" + 
						"						 int three, int four,\n" + 
						"						 int five) {\n" + 
						"		\n" + 
						"		return 0;\n" + 
						"	}");
		
		String indent= fScanner.computeIndentation(fDocument.getLength() - 1).toString();
		Assert.assertEquals("	", indent);
	}
	
	public void testBlocksInCaseStatements() {
		fDocument.set(
				"		switch (i) {\n" + 
				"			case 1:\n" + 
				"				new Runnable() {\n" + 
				"");
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("					", indent);
	}
	
	public void testAnonymousTypeBraceNextLine() throws Exception {
		fDocument.set(
				"		MenuItem mi= new MenuItem(\"About...\");\n" + 
				"		mi.addActionListener(new ActionListener() " +
				"		{\n" 
				);

		String indent= fScanner.computeIndentation(fDocument.getLength() - 2).toString();
		Assert.assertEquals("		", indent);

    }

	/*
	 * @since 3.2 
	 */
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
	
	public void testShiftOperator() throws Exception {
		fDocument.set(
				"		for (int j = 0; j == 0; j ++) {\n" + 
				"			j = 3 >>> 1;\n" 
		);
		
		String indent= fScanner.computeIndentation(fDocument.getLength()).toString();
		Assert.assertEquals("\t\t\t", indent);
	}
}

