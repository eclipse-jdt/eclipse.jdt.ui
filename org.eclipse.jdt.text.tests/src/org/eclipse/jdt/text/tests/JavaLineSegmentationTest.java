/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.preference.PreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.tests.TestTextViewer;

import org.eclipse.jdt.ui.text.JavaTextTools;

import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.IJavaPartitions;



public class JavaLineSegmentationTest extends TestCase {
	
	protected TestTextViewer fTextViewer;
	protected IDocument fDocument;
	protected JavaTextTools fTextTools;
	
	public JavaLineSegmentationTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		return new TestSuite(JavaLineSegmentationTest.class); 
	}
	
	protected void setUp() {
		fTextViewer= new TestTextViewer();
		fTextTools= new JavaTextTools(new PreferenceStore());
		
		fDocument= new Document();
		IDocumentPartitioner partitioner= fTextTools.createDocumentPartitioner();
		partitioner.connect(fDocument);
		if (fDocument instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) fDocument;
			extension3.setDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING, partitioner);
		} else {
			fDocument.setDocumentPartitioner(partitioner);
		}
	}
	
	protected void tearDown () {
		fTextTools.dispose();
		fTextTools= null;
		
		IDocumentPartitioner partitioner;
		if (fDocument instanceof IDocumentExtension3) {
			IDocumentExtension3 extension3= (IDocumentExtension3) fDocument;
			partitioner= extension3.getDocumentPartitioner(IJavaPartitions.JAVA_PARTITIONING);
		} else {
			partitioner= fDocument.getDocumentPartitioner();
		}
		partitioner.disconnect();
		fDocument= null;
	}
		
	void checkSegmentation(int[] result, int[] expectation) {
		if (expectation == null) {
			assertTrue("invalid segments", result == null);
		} else {
			assertTrue(result != null);
			assertTrue("invalid number of segments", expectation.length == result.length);
			for (int i= 0; i < expectation.length; i++)
				assertTrue(result[i] + " != " + expectation[i], expectation[i] == result[i]);
		}
	}
	
	
	public void test11() {
		fDocument.set("abcde");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, null);
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test12() {
		fDocument.set("abcde\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, null);
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test13() {
		fDocument.set("\nabcde");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, null);
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test14() {
		fDocument.set("\nabcde\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, null);
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	
	public void test21() {
		fDocument.set("\"ab\"cde");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test22() {
		fDocument.set("\"ab\"cde\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test23() {
		fDocument.set("\n\"ab\"cde");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test24() {
		fDocument.set("\n\"ab\"cde\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	
	public void test31() {
		fDocument.set("\"ab\"c\"de\"");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4, 5 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test32() {
		fDocument.set("\"ab\"c\"de\"\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4, 5 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test33() {
		fDocument.set("\n\"ab\"c\"de\"");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4, 5 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test34() {
		fDocument.set("\n\"ab\"c\"de\"\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4, 5 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	
	public void test41() {
		fDocument.set("\"ab\"\"cd\"e");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4, 8 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test42() {
		fDocument.set("\"ab\"\"cd\"e\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4, 8 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test43() {
		fDocument.set("\n\"ab\"\"cd\"e");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4, 8 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test44() {
		fDocument.set("\n\"ab\"\"cd\"e\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4, 8 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	

	public void test51() {
		fDocument.set("\"ab\"\"cde\"");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test52() {
		fDocument.set("\"ab\"\"cde\"\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test53() {
		fDocument.set("\n\"ab\"\"cde\"");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test54() {
		fDocument.set("\n\"ab\"\"cde\"\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0, 4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}


	public void test61() {
		fDocument.set("\"abcde\"");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test62() {
		fDocument.set("\"abcde\"\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test63() {
		fDocument.set("\n\"abcde\"");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test64() {
		fDocument.set("\n\"abcde\"\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] { 0 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test71() {
		fDocument.set("ab\"\"cde");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] { 0,  2,  4});
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test72() {
		fDocument.set("ab\"\"cde\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 0);
			checkSegmentation(result, new int[] {  0,  2,  4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test73() {
		fDocument.set("\nab\"\"cde");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] {  0,  2,  4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
	
	public void test74() {
		fDocument.set("\nab\"\"cde\n");
		fTextViewer.setDocument(fDocument);
		try {
			int[] result= JavaEditor.getBidiLineSegments(fTextViewer, 1);
			checkSegmentation(result, new int[] {  0,  2,  4 });
		} catch (BadLocationException x) {
			assertTrue(false);
		}
	}
}
