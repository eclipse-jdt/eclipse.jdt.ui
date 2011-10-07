/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
public class JavaAutoIndentStrategyTest extends TestCase implements ILogListener {

	public static Test suite() {
		return new TestSuite(JavaAutoIndentStrategyTest.class);
	}

	private FastPartitioner fPartitioner;

	private Document fDocument;

	private DocumentCommand fDocumentCommand;

	private Accessor fAccessor;

	private JavaAutoIndentStrategy fJavaAutoIndentStrategy;

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
	}

	private void performPaste() {
		fAccessor.invoke("smartPaste", new Class[] { IDocument.class, DocumentCommand.class }, new Object[] { fDocument, fDocumentCommand });
	}

	public void testPasteDefaultAtEnd() {
		fDocument.set("public class Test2 {\r\n\r\n}\r\n");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 27;
		fDocumentCommand.text= "default";
		performPaste();
		String result= "default";
		Assert.assertEquals(result, fDocumentCommand.text);
	}

	public void testPasteFooAtEnd() {
		fDocument.set("public class Test2 {\r\n\r\n}\r\n");

		fDocumentCommand.doit= true;
		fDocumentCommand.offset= 27;
		fDocumentCommand.text= "foo";
		performPaste();
		String result= "foo";
		Assert.assertEquals(result, fDocumentCommand.text);
	}

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
		Assert.assertEquals(result, fDocumentCommand.text);
	}

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
		Assert.assertEquals(result, fDocumentCommand.text);
	}

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
		Assert.assertEquals(result, fDocumentCommand.text);
	}

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
		Assert.assertEquals(result, fDocumentCommand.text);
	}

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
		Assert.assertEquals(result, fDocumentCommand.text);
	}

	/*
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		JavaPlugin.getDefault().getLog().addLogListener(this);
	}

	/*
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		JavaPlugin.getDefault().getLog().removeLogListener(this);
	}

	/*
	 * @see org.eclipse.core.runtime.ILogListener#logging(org.eclipse.core.runtime.IStatus, java.lang.String)
	 */
	public void logging(IStatus status, String plugin) {
		fail();
	}
}
