/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.jdt.internal.core.codemanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.core.codemanipulation.TextBuffer;
import org.eclipse.jdt.internal.core.codemanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.core.codemanipulation.TextEdit;
import org.eclipse.jdt.internal.core.codemanipulation.TextPosition;

import org.eclipse.jdt.testplugin.TestPluginLauncher;

public class TextBufferTest extends TestCase {

	private static final Class THIS= TextBufferTest.class;
	
	private TextBufferEditor fEditor;
	
	public TextBufferTest(String name) {
		super(name);
	}
	
	public static void main(String[] args) {
		TestPluginLauncher.run(TestPluginLauncher.getLocationFromProperties(), THIS, args);
	}

	public static Test suite() {
		return new TestSuite(THIS);
	}
	
	protected void setUp() throws Exception {
		fEditor= new TextBufferEditor(TextBuffer.create("0123456789"));
	}
	
	protected void tearDown() throws Exception {
		fEditor= null;
	}
	
	public void testOverlap1() throws Exception {
		// [ [ ] ]
		fEditor.addTextEdit(SimpleTextEdit.createReplace(0, 2, "01"));
		fEditor.addTextEdit(SimpleTextEdit.createReplace(1, 2, "12"));
		assertTrue(!fEditor.canPerformEdits());
	}	
	
	public void testOverlap2() throws Exception {
		// [[ ] ]
		fEditor.addTextEdit(SimpleTextEdit.createReplace(0, 2, "01"));
		fEditor.addTextEdit(SimpleTextEdit.createReplace(0, 1, "0"));
		assertTrue(!fEditor.canPerformEdits());
	}	
	
	public void testOverlap3() throws Exception {
		// [ [ ]]
		fEditor.addTextEdit(SimpleTextEdit.createReplace(0, 2, "01"));
		fEditor.addTextEdit(SimpleTextEdit.createReplace(1, 1, "1"));
		assertTrue(!fEditor.canPerformEdits());
	}	
	
	public void testOverlap4() throws Exception {
		// [ [ ] ]
		fEditor.addTextEdit(SimpleTextEdit.createReplace(0, 3, "012"));
		fEditor.addTextEdit(SimpleTextEdit.createReplace(1, 1, "1"));
		assertTrue(!fEditor.canPerformEdits());
	}
	
	public void testOverlap5() throws Exception {
		// [ []  ]
		fEditor.addTextEdit(SimpleTextEdit.createReplace(0, 3, "012"));
		fEditor.addTextEdit(SimpleTextEdit.createInsert(1, "xx"));
		assertTrue(!fEditor.canPerformEdits());
	}
	
	public void testOverlap6() throws Exception {
		// [  [] ]
		fEditor.addTextEdit(SimpleTextEdit.createReplace(0, 3, "012"));
		fEditor.addTextEdit(SimpleTextEdit.createInsert(2, "xx"));
		assertTrue(!fEditor.canPerformEdits());
	}
	
	public void testInsert1() throws Exception {
		// [][  ]
		TextEdit e1= SimpleTextEdit.createInsert(2, "yy");
		TextEdit e2= SimpleTextEdit.createReplace(2, 3, "3456");
		fEditor.addTextEdit(e1);
		fEditor.addTextEdit(e2);
		assertTrue(fEditor.canPerformEdits());
		fEditor.performEdits(new NullProgressMonitor());
		assert(e1.getTextPositions()[0], 2, 2);
		assert(e2.getTextPositions()[0], 4, 4);
	}
	
	public void testInsert2() throws Exception {
		// [][]
		TextEdit e1= SimpleTextEdit.createInsert(2, "yy");
		TextEdit e2= SimpleTextEdit.createInsert(2, "xx");
		fEditor.addTextEdit(e1);
		fEditor.addTextEdit(e2);
		assertTrue(fEditor.canPerformEdits());
		fEditor.performEdits(new NullProgressMonitor());
		assert(e1.getTextPositions()[0], 4, 2);
		assert(e2.getTextPositions()[0], 2, 2);
	}
	
	public void testInsert3() throws Exception {
		// [  ][][  ]
		TextEdit e1= SimpleTextEdit.createReplace(0, 2, "011");
		TextEdit e2= SimpleTextEdit.createInsert(2, "xx");
		TextEdit e3= SimpleTextEdit.createReplace(2, 2, "2");
		fEditor.addTextEdit(e1);
		fEditor.addTextEdit(e2);
		fEditor.addTextEdit(e3);
		assertTrue(fEditor.canPerformEdits());
		fEditor.performEdits(new NullProgressMonitor());
		assert(e1.getTextPositions()[0], 0, 3);
		assert(e2.getTextPositions()[0], 3, 2);
		assert(e3.getTextPositions()[0], 5, 1);
	}
	
	private void assert(TextPosition p, int offset, int length) {
		assertTrue(p.offset == offset && p.length == length);
	}	
}

