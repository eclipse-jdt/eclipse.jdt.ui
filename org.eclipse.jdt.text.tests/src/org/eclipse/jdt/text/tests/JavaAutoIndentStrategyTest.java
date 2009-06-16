/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

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
	}

	public void testPasteDefaultAtEnd() {
		fDocument.set("public class Test2 {\r\n\r\n}\r\n");
		JavaAutoIndentStrategy javaAutoIndentStrategy= new JavaAutoIndentStrategy(IJavaPartitions.JAVA_PARTITIONING, null, null);
		DocumentCommand documentCommand= new DocumentCommand() {
		};
		documentCommand.doit= true;
		documentCommand.offset= 27;
		documentCommand.text= "default";

		Accessor accessor= new Accessor(javaAutoIndentStrategy, JavaAutoIndentStrategy.class);
		accessor.invoke("smartPaste", new Class[] { IDocument.class, DocumentCommand.class }, new Object[] { fDocument, documentCommand });

	}

	public void testPasteFooAtEnd() {
		fDocument.set("public class Test2 {\r\n\r\n}\r\n");
		JavaAutoIndentStrategy javaAutoIndentStrategy= new JavaAutoIndentStrategy(IJavaPartitions.JAVA_PARTITIONING, null, null);
		DocumentCommand documentCommand= new DocumentCommand() {
		};
		documentCommand.doit= true;
		documentCommand.offset= 27;
		documentCommand.text= "foo";

		Accessor accessor= new Accessor(javaAutoIndentStrategy, JavaAutoIndentStrategy.class);
		accessor.invoke("smartPaste", new Class[] { IDocument.class, DocumentCommand.class }, new Object[] { fDocument, documentCommand });

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
