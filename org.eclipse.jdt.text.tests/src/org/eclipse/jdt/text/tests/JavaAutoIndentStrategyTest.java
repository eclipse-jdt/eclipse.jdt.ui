/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentCommand;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.FastPartitioner;

import org.eclipse.jdt.ui.text.IJavaPartitions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.text.FastJavaPartitionScanner;
import org.eclipse.jdt.internal.ui.text.java.JavaAutoIndentStrategy;

/**
 * JavaAutoIndentStrategyTest.
 *
 * @since 3.6
 */
public class JavaAutoIndentStrategyTest implements ILogListener {
	private final FastPartitioner fPartitioner;

	private final Document fDocument;

	private final DocumentCommand fDocumentCommand;

	private final Accessor fAccessor;

	private final Accessor fCommandAccessor;

	private final JavaAutoIndentStrategy fJavaAutoIndentStrategy;

	public JavaAutoIndentStrategyTest() {
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
		fJavaAutoIndentStrategy= new JavaAutoIndentStrategy(IJavaPartitions.JAVA_PARTITIONING, null, null);
		fDocumentCommand= new DocumentCommand() {
		};
		fAccessor= new Accessor(fJavaAutoIndentStrategy, JavaAutoIndentStrategy.class);
		fCommandAccessor= new Accessor(fDocumentCommand, DocumentCommand.class);
	}

	private void performPaste() {
		fAccessor.invoke("smartPaste", new Class[] { IDocument.class, DocumentCommand.class }, new Object[] { fDocument, fDocumentCommand });
	}

	@Test
	public void testPasteDefaultAtEnd() {
		fDocument.set("public class Test2 {\r\n\r\n}\r\n");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 27;
		fDocumentCommand.text= "default";
		performPaste();
		String result= "default";
		assertEquals(result, fDocumentCommand.text);
	}

	@Test
	public void testPasteFooAtEnd() {
		fDocument.set("public class Test2 {\r\n\r\n}\r\n");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 27;
		fDocumentCommand.text= "foo";
		performPaste();
		String result= "foo";
		assertEquals(result, fDocumentCommand.text);
	}

	@Test
	public void testPasteAndIndentOfLongStringWithContinuations1() {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=330556
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=65317
		fDocument.set("public class Test2 {\n}");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "String[]a=new String[] {\n" +
				"\"X.java\",\n" +
				"\"public class X extends B{\\n\"\n" +
				"\"    public static int field1;\\n\"\n" +
				"\"    public static X xfield;\\n\"\n" +
				"\"    public void bar1(int i) {\\n\"\n" +
				"\"        field1 = 1;\\n\"\n" +
				"\"    }\\n\"\n" +
				"\"    public void bar2(int i) {\\n\"\n" +
				"\"        this.field1 = 1;\\n\"\n" +
				"\"    }\\n\"\n" +
				"\"}\\n\"\n" +
				"\"class A{\\n\"\n" +
				"\"    public static X xA;\\n\"\n" +
				"\"}\\n\"\n" +
				"\"class B{\\n\"\n" +
				"\"    public static int b1;\\n\"\n" +
				"\"}\"\n" +
				"};";
		performPaste();
		String result= "\tString[]a=new String[] {\n" +
				"\t\t\t\"X.java\",\n" +
				"\t\t\t\"public class X extends B{\\n\"\n" +
				"\t\t\t\"    public static int field1;\\n\"\n" +
				"\t\t\t\"    public static X xfield;\\n\"\n" +
				"\t\t\t\"    public void bar1(int i) {\\n\"\n" +
				"\t\t\t\"        field1 = 1;\\n\"\n" +
				"\t\t\t\"    }\\n\"\n" +
				"\t\t\t\"    public void bar2(int i) {\\n\"\n" +
				"\t\t\t\"        this.field1 = 1;\\n\"\n" +
				"\t\t\t\"    }\\n\"\n" +
				"\t\t\t\"}\\n\"\n" +
				"\t\t\t\"class A{\\n\"\n" +
				"\t\t\t\"    public static X xA;\\n\"\n" +
				"\t\t\t\"}\\n\"\n" +
				"\t\t\t\"class B{\\n\"\n" +
				"\t\t\t\"    public static int b1;\\n\"\n" +
				"\t\t\t\"}\"\n" +
				"\t\t\t};";
		assertEquals(result, fDocumentCommand.text);
	}

	@Test
	public void testPasteAndIndentOfStringWithContinuations2() {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=337150
		fDocument.set("public class Test2 {\n}");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "String array2= \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\";\n";
		performPaste();
		String result= "\tString array2= \"this is the 1st string\"\n" +
				"\t\t\t+ \"this is the 1st string\"\n" +
				"\t\t\t+ \"this is the 1st string\";\n";
		assertEquals(result, fDocumentCommand.text);
	}

	@Test
	public void testPasteAndIndentOfStringWithContinuations3() {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=337150
		fDocument.set("public class Test2 {\n}");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "\tString array2= \"this is the 1st string\"\n" +
				"+ \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\";\n";
		performPaste();
		String result= "\tString array2= \"this is the 1st string\"\n" +
				"+ \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\";\n";
		assertEquals(result, fDocumentCommand.text);
	}

	@Test
	public void testPasteAndIndentOfStringWithContinuations4() {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=337150
		fDocument.set("public class Test2 {\n}");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "\tString array2= \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\";\n";
		performPaste();
		String result= "\tString array2= \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\"\n" +
				"\t+ \"this is the 1st string\";\n";
		assertEquals(result, fDocumentCommand.text);
	}

	@Test
	public void testPasteAndIndentOfStringWithContinuations5() {
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=337150
		fDocument.set("public class Test2 {\n}");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "\tString array2= \"this is the 1st string\"\n" +
				"\t\t\t\t\t+ \"this is the 1st string\"\n" +
				"\t\t\t\t\t\t\t+ \"this is the 1st string\";\n";
		performPaste();
		String result= "\tString array2= \"this is the 1st string\"\n" +
				"\t\t\t+ \"this is the 1st string\"\n" +
				"\t\t\t\t\t+ \"this is the 1st string\";\n";
		assertEquals(result, fDocumentCommand.text);
	}

	private void performSmartIndentAfterNewLine() {
		fAccessor.invoke("clearCachedValues", null, null);
		fAccessor.invoke("smartIndentAfterNewLine", new Class[] { IDocument.class, DocumentCommand.class }, new Object[] { fDocument, fDocumentCommand });
		fCommandAccessor.invoke("execute", new Class[] { IDocument.class }, new Object[] { fDocument });
	}

	@Test
	public void testSmartIndentAfterNewLine1() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=29379
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("main (new String [] {);");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("main (new String [] {\r\n");
		buf.append("\t\t\r\n");
		buf.append("});");
		assertEquals(buf.toString(), fDocument.get());

		fDocument.set("main (new String [] {\"a\");");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 24;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf1= new StringBuilder();
		buf1.append("main (new String [] {\"a\"\r\n");
		buf1.append("\t\t\r\n");
		buf1.append("});");
		assertEquals(buf1.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine2() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395071
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("main (new String [] {\"a\",);");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 25;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("main (new String [] {\"a\",\r\n");
		buf.append("\t\t\r\n");
		buf.append("});");
		assertEquals(buf.toString(), fDocument.get());

		fDocument.set("main (new String [] {\"a\", );");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 26;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf1= new StringBuilder();
		buf1.append("main (new String [] {\"a\", \r\n");
		buf1.append("\t\t\r\n");
		buf1.append("});");
		assertEquals(buf1.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine3() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=395071
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("main (new String [] {\"a\",\"b\",);");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 29;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("main (new String [] {\"a\",\"b\",\r\n");
		buf.append("\t\t\r\n");
		buf.append("});");
		assertEquals(buf.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine4() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=254704
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("@NamedQueries({);");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 15;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("@NamedQueries({\r\n");
		buf.append("\t\r\n");
		buf.append("});");
		assertEquals(buf.toString(), fDocument.get());

		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=394467
		fDocument.set("@MesageDriven( activationConfig ={)");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 34;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf1= new StringBuilder();
		buf1.append("@MesageDriven( activationConfig ={\r\n");
		buf1.append("\t\t\r\n");
		buf1.append("})");
		assertEquals(buf1.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine5() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=256087
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("if (false) {return false;");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 12;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("if (false) {\r\n");
		buf.append("\treturn false;\r\n");
		buf.append("}");
		assertEquals(buf.toString(), fDocument.get());

		fDocument.set("if (false) { return false;");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 13;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf1= new StringBuilder();
		buf1.append("if (false) { \r\n");
		buf1.append("\treturn false;\r\n");
		buf1.append("}");
		assertEquals(buf1.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine6() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=200015
		fDocument.setInitialLineDelimiter("\r\n");
		StringBuilder inBuf= new StringBuilder();
		inBuf.append("enum ReviewResult {\n");
		inBuf.append("    Good{, Bad\n");
		inBuf.append("}");
		fDocument.set(inBuf.toString());
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 29;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("enum ReviewResult {\n");
		buf.append("    Good{\r\n");
		buf.append("    \t\n");
		buf.append("    }, Bad\n");
		buf.append("}");
		assertEquals(buf.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine7() {
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("int[] a= new int[] { ;");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("int[] a= new int[] { \r\n");
		buf.append("\t\t\r\n");
		buf.append("};");
		assertEquals(buf.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine8() {
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("String[] strs = {\"a\",\"b\",");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 21;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("String[] strs = {\"a\",\r\n");
		buf.append("\t\t\"b\",\r\n");
		buf.append("}");
		assertEquals(buf.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine9() {
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("{ int a;");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 1;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("{\r\n");
		buf.append("\tint a;\r\n");
		buf.append("}");
		assertEquals(buf.toString(), fDocument.get());
	}

	@Test
	public void testSmartIndentAfterNewLine10() {
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=404879
		fDocument.setInitialLineDelimiter("\r\n");
		fDocument.set("{ foo();");
		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 1;
		fDocumentCommand.text= "\r\n";
		performSmartIndentAfterNewLine();
		StringBuilder buf= new StringBuilder();
		buf.append("{\r\n");
		buf.append("\tfoo();\r\n");
		buf.append("}");
		assertEquals(buf.toString(), fDocument.get());
	}

	@Before
	public void setUp() throws Exception {
		JavaPlugin.getDefault().getLog().addLogListener(this);
	}

	@After
	public void tearDown() throws Exception {
		JavaPlugin.getDefault().getLog().removeLogListener(this);
	}

	/*
	 * @see org.eclipse.core.runtime.ILogListener#logging(org.eclipse.core.runtime.IStatus, java.lang.String)
	 */
	@Override
	public void logging(IStatus status, String plugin) {
		fail();
	}
}
