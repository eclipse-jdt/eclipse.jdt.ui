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

package org.eclipse.jdt.ui.tests.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.corext.textmanipulation.CopySourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.CopyTargetEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.EditProcessor;
import org.eclipse.jdt.internal.corext.textmanipulation.MalformedTreeException;
import org.eclipse.jdt.internal.corext.textmanipulation.MoveSourceEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.MoveTargetEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.MultiTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.RangeMarker;
import org.eclipse.jdt.internal.corext.textmanipulation.SimpleTextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBuffer;
import org.eclipse.jdt.internal.corext.textmanipulation.TextBufferEditor;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEdit;
import org.eclipse.jdt.internal.corext.textmanipulation.TextEditCopier;
import org.eclipse.jdt.internal.corext.textmanipulation.TextRange;
import org.eclipse.jdt.internal.corext.textmanipulation.UndoMemento;

public class TextBufferTest extends TestCase {

	private static final Class THIS= TextBufferTest.class;
	
	private IDocument fDocument;
	private EditProcessor fProcessor;
	
	public TextBufferTest(String name) {
		super(name);
	}

	public static Test allTests() {
		return new TestSuite(THIS);
	}

	public static Test suite() {
		TestSuite result= new TestSuite(THIS);
		if (false) {	// For hot code replace when debugging test cases
			result.addTestSuite(THIS);
			result.addTestSuite(THIS);
			result.addTestSuite(THIS);
			result.addTestSuite(THIS);
			result.addTestSuite(THIS);
			result.addTestSuite(THIS);
		}
		return result;
	}
	
	protected void setUp() throws Exception {
		fDocument= new Document("0123456789");
		fProcessor= new EditProcessor(fDocument);
	}
	
	protected void tearDown() throws Exception {
		fProcessor= null;
	}
	
	public void testOverlap1() throws Exception {
		// [ [ ] ]
		fProcessor.add(SimpleTextEdit.createReplace(0, 2, "01"));
		boolean exception= false;
		try {
			fProcessor.add(SimpleTextEdit.createReplace(1, 2, "12"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}	
	
	public void testOverlap2() throws Exception {
		// [[ ] ]
		fProcessor.add(SimpleTextEdit.createReplace(0, 2, "01"));
		boolean exception= false;
		try {
			fProcessor.add(SimpleTextEdit.createReplace(0, 1, "0"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}	
	
	public void testOverlap3() throws Exception {
		// [ [ ]]
		fProcessor.add(SimpleTextEdit.createReplace(0, 2, "01"));
		boolean exception= false;
		try {
			fProcessor.add(SimpleTextEdit.createReplace(1, 1, "1"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}	
	
	public void testOverlap4() throws Exception {
		// [ [ ] ]
		fProcessor.add(SimpleTextEdit.createReplace(0, 3, "012"));
		boolean exception= false;
		try {
			fProcessor.add(SimpleTextEdit.createReplace(1, 1, "1"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap5() throws Exception {
		// [ []  ]
		fProcessor.add(SimpleTextEdit.createReplace(0, 3, "012"));
		boolean exception= false;
		try {
			fProcessor.add(SimpleTextEdit.createInsert(1, "xx"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap6() throws Exception {
		// [  [] ]
		fProcessor.add(SimpleTextEdit.createReplace(0, 3, "012"));
		boolean exception= false;
		try {
			fProcessor.add(SimpleTextEdit.createInsert(2, "xx"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap7() throws Exception {
		MoveSourceEdit source= new MoveSourceEdit(2, 5);
		MoveTargetEdit target= new MoveTargetEdit(3, source);
		fProcessor.add(source);
		boolean exception= false;
		try {
			fProcessor.add(target);
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap8() throws Exception {
		MoveSourceEdit source= new MoveSourceEdit(2, 5);
		MoveTargetEdit target= new MoveTargetEdit(6, source);
		fProcessor.add(source);
		boolean exception= false;
		try {
			fProcessor.add(target);
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap9() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(3, 1);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		MoveSourceEdit s2= new MoveSourceEdit(2, 3);
		MoveTargetEdit t2= new MoveTargetEdit(8, s2);
		fProcessor.add(s1);
		fProcessor.add(t1);
		boolean exception= false;
		try {
			fProcessor.add(s2);
			fProcessor.add(t2);
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testUnconnected1() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(3, 1);
		boolean exception= false;
		try {
			fProcessor.add(s1);
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testCopy1() throws Exception {
		MultiTextEdit root= new MultiTextEdit();
		SimpleTextEdit e1= SimpleTextEdit.createInsert(2, "yy");
		SimpleTextEdit e2= SimpleTextEdit.createReplace(2, 3, "3456");
		root.add(e1);
		root.add(e2);
		List org= flatten(root);
		TextEditCopier copier= new TextEditCopier(root);
		List copy= flatten(copier.perform());
		compare(copier, org, copy);
	}
	
	public void testCopy2() throws Exception {
		MultiTextEdit root= new MultiTextEdit();
		CopySourceEdit s1= new CopySourceEdit(5, 2);
		CopyTargetEdit t1= new CopyTargetEdit(8, s1);
		CopySourceEdit s2= new CopySourceEdit(5, 2);
		CopyTargetEdit t2= new CopyTargetEdit(2, s2);
		s1.add(s2);
		root.add(s1);
		root.add(t1);
		root.add(t2);
		List org= flatten(root);
		TextEditCopier copier= new TextEditCopier(root);
		List copy= flatten(copier.perform());
		compare(copier, org, copy);
	}
		
	private List flatten(TextEdit edit) {
		List result= new ArrayList();
		flatten(result, edit);
		return result;
	}
	
	private static void flatten(List result, TextEdit edit) {
		result.add(edit);
		TextEdit[] children= edit.getChildren();
		for (int i= 0; i < children.length; i++) {
			flatten(result, children[i]);
		}
	}
	
	private static void compare(TextEditCopier copier, List org, List copy) {
		assertTrue("Same length", org.size() == copy.size());
		for (Iterator iter= copy.iterator(); iter.hasNext();) {
			TextEdit edit= (TextEdit)iter.next();
			assertTrue("Original is part of copy list", !org.contains(edit));
			if (edit instanceof MoveSourceEdit) {
				MoveSourceEdit source= (MoveSourceEdit)edit;
				assertTrue("Target edit isn't a copy", copy.contains(source.getTargetEdit()));
				assertTrue("Traget edit is a original", !org.contains(source.getTargetEdit()));
			} else if (edit instanceof MoveTargetEdit) {
				MoveTargetEdit target= (MoveTargetEdit)edit;
				assertTrue("Source edit isn't a copy", copy.contains(target.getSourceEdit()));
				assertTrue("Source edit is a original", !org.contains(target.getSourceEdit()));
			} else if (edit instanceof CopySourceEdit) {
				CopySourceEdit source= (CopySourceEdit)edit;
				assertTrue("Target edit isn't a copy", copy.contains(source.getTargetEdit()));
				assertTrue("Traget edit is a original", !org.contains(source.getTargetEdit()));
			} else if (edit instanceof CopyTargetEdit) {
				CopyTargetEdit target= (CopyTargetEdit)edit;
				assertTrue("Source edit isn't a copy", copy.contains(target.getSourceEdit()));
				assertTrue("Source edit is a original", !org.contains(target.getSourceEdit()));
			}
		}
	}
		
	public void testInsert1() throws Exception {
		// [][  ]
		SimpleTextEdit e1= SimpleTextEdit.createInsert(2, "yy");
		SimpleTextEdit e2= SimpleTextEdit.createReplace(2, 3, "3456");
		fProcessor.add(e1);
		fProcessor.add(e2);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(e1.getTextRange(), 2, 2);
		assertEquals(e2.getTextRange(), 4, 4);
		assertEquals("Buffer content", "01yy345656789", fDocument.get());
		doUndoRedo(undo, "01yy345656789");
	}
	
	public void testInsert2() throws Exception {
		// [][]
		SimpleTextEdit e1= SimpleTextEdit.createInsert(2, "yy");
		SimpleTextEdit e2= SimpleTextEdit.createInsert(2, "xx");
		fProcessor.add(e1);
		fProcessor.add(e2);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(e1.getTextRange(), 2, 2);
		assertEquals(e2.getTextRange(), 4, 2);
		assertEquals("Buffer content", "01yyxx23456789", fDocument.get());
		doUndoRedo(undo, "01yyxx23456789");
	}
	
	public void testInsert3() throws Exception {
		// [  ][][  ]
		SimpleTextEdit e1= SimpleTextEdit.createReplace(0, 2, "011");
		SimpleTextEdit e2= SimpleTextEdit.createInsert(2, "xx");
		SimpleTextEdit e3= SimpleTextEdit.createReplace(2, 2, "2");
		fProcessor.add(e1);
		fProcessor.add(e2);
		fProcessor.add(e3);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(e1.getTextRange(), 0, 3);
		assertEquals(e2.getTextRange(), 3, 2);
		assertEquals(e3.getTextRange(), 5, 1);
		assertEquals("Buffer content", "011xx2456789", fDocument.get());
		doUndoRedo(undo, "011xx2456789");
	}
	
	public void testInsert4() throws Exception {
		SimpleTextEdit e1= SimpleTextEdit.createInsert(0, "xx");
		fProcessor.add(e1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer length", 12, fDocument.getLength());
		assertEquals(e1.getTextRange(), 0, 2);
		assertEquals("Buffer content", "xx0123456789", fDocument.get());
		doUndoRedo(undo, "xx0123456789");
	}
	
	public void testInsert5() throws Exception {
		SimpleTextEdit e1= SimpleTextEdit.createInsert(10, "xx");
		fProcessor.add(e1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer length", 12, fDocument.getLength());
		assertEquals(e1.getTextRange(), 10, 2);
		assertEquals("Buffer content", "0123456789xx", fDocument.get());
		doUndoRedo(undo, "0123456789xx");
	}
	
	public void testInsertReplace1() throws Exception {
		SimpleTextEdit e1= SimpleTextEdit.createReplace(2, 1, "y");
		SimpleTextEdit e2= SimpleTextEdit.createInsert(2, "xx");
		fProcessor.add(e1);
		fProcessor.add(e2);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(e1.getTextRange(), 4, 1);
		assertEquals(e2.getTextRange(), 2, 2);
		assertEquals("Buffer content", "01xxy3456789", fDocument.get());
		doUndoRedo(undo, "01xxy3456789");
	}
	
	public void testDelete1() throws Exception {
		SimpleTextEdit e1= SimpleTextEdit.createDelete(3, 1);
		fProcessor.add(e1);
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(e1.getTextRange(), 3, 0);
		assertEquals("Buffer content", "012456789", fDocument.get());
		doUndoRedo(undo, "012456789");
	}
	
	public void testDelete2() throws Exception {
		SimpleTextEdit e1= SimpleTextEdit.createDelete(4, 1);
		SimpleTextEdit e2= SimpleTextEdit.createDelete(3, 1);
		SimpleTextEdit e3= SimpleTextEdit.createDelete(5, 1);
		fProcessor.add(e1);
		fProcessor.add(e2);
		fProcessor.add(e3);
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(e1.getTextRange(), 3, 0);
		assertEquals(e2.getTextRange(), 3, 0);
		assertEquals(e3.getTextRange(), 3, 0);
		assertEquals("Buffer content", "0126789", fDocument.get());
		doUndoRedo(undo, "0126789");
	}
	
	public void testDelete3() throws Exception {
		SimpleTextEdit e1= SimpleTextEdit.createInsert(3, "x");
		SimpleTextEdit e2= SimpleTextEdit.createDelete(3, 1);
		fProcessor.add(e1);
		fProcessor.add(e2);
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(e1.getTextRange(), 3, 1);
		assertEquals(e2.getTextRange(), 4, 0);
		assertEquals("Buffer content", "012x456789", fDocument.get());
		doUndoRedo(undo, "012x456789");
	}
	
	public void testDeleteWithChildren() throws Exception {
		SimpleTextEdit e1= SimpleTextEdit.createDelete(2, 6);
		MultiTextEdit e2= new MultiTextEdit(3, 3);
		e1.add(e2);
		SimpleTextEdit e3= SimpleTextEdit.createReplace(3,1,"xx");
		SimpleTextEdit e4= SimpleTextEdit.createReplace(5,1,"yy");
		e2.add(e3);
		e2.add(e4);
		fProcessor.add(e1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0189", fDocument.get());
		assertTrue(e2.isDeleted());
		assertTrue(e3.isDeleted());
		assertTrue(e4.isDeleted());
		doUndoRedo(undo, "0189");
	}
	
	public void testMove1() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0142356789", fDocument.get());
		assertEquals(s1.getTextRange(), 2, 0);
		assertEquals(t1.getTextRange(), 3, 2);
		doUndoRedo(undo, "0142356789");
	}
	
	public void testMove2() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(5, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0156234789", fDocument.get());
		assertEquals(s1.getTextRange(), 7, 0);
		assertEquals(t1.getTextRange(), 2, 2);
		doUndoRedo(undo, "0156234789");
	}

	public void testMove3() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		SimpleTextEdit e2= SimpleTextEdit.createReplace(4, 1, "x");
		fProcessor.add(s1);
		fProcessor.add(t1);
		fProcessor.add(e2);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "01x5623789", fDocument.get());
		assertEquals(s1.getTextRange(), 2, 0);
		assertEquals(t1.getTextRange(), 5, 2);
		assertEquals(e2.getTextRange(), 2, 1);
		doUndoRedo(undo, "01x5623789");
	}
	
	public void testMove4() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(7, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		SimpleTextEdit e2= SimpleTextEdit.createReplace(5, 1, "x");
		fProcessor.add(s1);
		fProcessor.add(t1);
		fProcessor.add(e2);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0178234x69", fDocument.get());
		assertEquals(s1.getTextRange(), 9, 0);
		assertEquals(t1.getTextRange(), 2, 2);
		assertEquals(e2.getTextRange(), 7, 1);
		doUndoRedo(undo, "0178234x69");
	}
	
	public void testMove5() throws Exception {
		// Move onto itself
		MoveSourceEdit s1= new MoveSourceEdit(2, 1);
		MoveTargetEdit t1= new MoveTargetEdit(3, s1);
		SimpleTextEdit e2= SimpleTextEdit.createReplace(2,1,"x");
		s1.add(e2);
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(s1.getTextRange(), 2, 0);
		assertEquals(t1.getTextRange(), 2, 1);
		assertEquals(e2.getTextRange(), 2, 1);
		assertEquals("Buffer content", "01x3456789", fDocument.get());
		doUndoRedo(undo, "01x3456789");
	}
	
	public void testMove6() throws Exception {
		// Move onto itself
		MoveSourceEdit s1= new MoveSourceEdit(2, 1);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		SimpleTextEdit e2= SimpleTextEdit.createReplace(2,1,"x");
		s1.add(e2);
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(s1.getTextRange(), 3, 0);
		assertEquals(t1.getTextRange(), 2, 1);
		assertEquals(e2.getTextRange(), 2, 1);
		assertEquals("Buffer content", "01x3456789", fDocument.get());
		doUndoRedo(undo,"01x3456789");
	}
	
	public void testMove7() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 3);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		SimpleTextEdit e2= SimpleTextEdit.createReplace(3, 1, "x");
		s1.add(e2);
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "01562x4789", fDocument.get());
		assertEquals(s1.getTextRange(), 2, 0);
		assertEquals(t1.getTextRange(), 4, 3);
		assertEquals(e2.getTextRange(), 5, 1);
		doUndoRedo(undo, "01562x4789");
	}
	
	public void testMove8() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(5, 3);
		MoveTargetEdit t1= new MoveTargetEdit(1, s1);
		SimpleTextEdit e2= SimpleTextEdit.createReplace(6, 1, "x");
		s1.add(e2);
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "05x7123489", fDocument.get());
		assertEquals(s1.getTextRange(), 8, 0);
		assertEquals(t1.getTextRange(), 1, 3);
		assertEquals(e2.getTextRange(), 2, 1);
		doUndoRedo(undo, "05x7123489");
	}
		
	public void testMove9() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(1, 3);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		
		MoveSourceEdit s2= new MoveSourceEdit(1, 1);
		MoveTargetEdit t2= new MoveTargetEdit(3, s2);
		s1.add(s2);
		s1.add(t2);
		
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(s1.getTextRange(), 1, 0);
		assertEquals(t1.getTextRange(), 2, 3);
		
		assertEquals(s2.getTextRange(), 2, 0);
		assertEquals(t2.getTextRange(), 3, 1);
		assertEquals("Buffer content", "0421356789", fDocument.get());
		doUndoRedo(undo, "0421356789");
	}
	
	public void testMove10() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(8, s1);
		MoveSourceEdit s2= new MoveSourceEdit(5, 2);
		MoveTargetEdit t2= new MoveTargetEdit(1, s2);
		
		fProcessor.add(s1);
		fProcessor.add(t1);
		fProcessor.add(s2);
		fProcessor.add(t2);
		
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(s1.getTextRange(), 4, 0);
		assertEquals(t1.getTextRange(), 6, 2);		
		assertEquals(s2.getTextRange(), 5, 0);
		assertEquals(t2.getTextRange(), 1, 2);
		assertEquals("Buffer content", "0561472389", fDocument.get());
		doUndoRedo(undo, "0561472389");		
	}
	
	public void testMoveWithRangeMarker() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		
		RangeMarker marker= new RangeMarker(2, 2);
		s1.add(marker);		
		
		fProcessor.add(s1);
		fProcessor.add(t1);
		
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0142356789", fDocument.get());
		assertEquals(s1.getTextRange(), 2, 0);
		assertEquals(t1.getTextRange(), 3, 2);
		assertEquals(marker.getTextRange(), 3, 2);
		doUndoRedo(undo, "0142356789");
	}
	
	public void testMoveWithTargetDelete() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 3);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		SimpleTextEdit e2= SimpleTextEdit.createDelete(6, 2);
		e2.add(t1);
		fProcessor.add(s1);
		fProcessor.add(e2);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "01589", fDocument.get());
		assertEquals(s1.getTextRange(), 2, 0);
		assertTrue(t1.isDeleted());
		assertEquals(e2.getTextRange(), 3, 0);
		doUndoRedo(undo, "01589");
	}
	
	public void testMoveUpWithSourceDelete() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(5, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		
		SimpleTextEdit d1= SimpleTextEdit.createDelete(5, 2);
		d1.add(s1);
		
		RangeMarker marker= new RangeMarker(5, 2);
		s1.add(marker);		
		
		fProcessor.add(d1);
		fProcessor.add(t1);
		
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0156234789", fDocument.get());
		assertEquals(t1.getTextRange(), 2, 2);
		assertEquals(marker.getTextRange(), 2, 2);
		assertTrue(s1.isDeleted());
		assertEquals(d1.getTextRange(), 7, 0);
		doUndoRedo(undo, "0156234789");
	}		
	
	public void testMoveDown() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		SimpleTextEdit i1= SimpleTextEdit.createInsert(5, "x");
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		SimpleTextEdit d1= SimpleTextEdit.createDelete(9, 1);
	
		RangeMarker m1= new RangeMarker(2, 2);
		s1.add(m1);
		
		fProcessor.add(s1);
		fProcessor.add(i1);
		fProcessor.add(t1);
		fProcessor.add(d1);
		
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "014x562378", fDocument.get());
		assertEquals(s1.getTextRange(), 2, 0);
		assertEquals(i1.getTextRange(), 3, 1);
		assertEquals(t1.getTextRange(), 6, 2);
		assertEquals(m1.getTextRange(), 6, 2);
		assertEquals(d1.getTextRange(), 10, 0);
		doUndoRedo(undo, "014x562378");
	}		
		
	public void testMoveUp() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(7, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		SimpleTextEdit i1= SimpleTextEdit.createInsert(5, "x");
		SimpleTextEdit d1= SimpleTextEdit.createDelete(9, 1);
	
		RangeMarker m1= new RangeMarker(7, 2);
		s1.add(m1);
		
		fProcessor.add(s1);
		fProcessor.add(i1);
		fProcessor.add(t1);
		fProcessor.add(d1);
		
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0178234x56", fDocument.get());
		assertEquals(s1.getTextRange(), 10, 0);
		assertEquals(i1.getTextRange(), 7, 1);
		assertEquals(t1.getTextRange(), 2, 2);
		assertEquals(m1.getTextRange(), 2, 2);
		assertEquals(d1.getTextRange(), 10, 0);
		doUndoRedo(undo, "0178234x56");
	}		
		
	public void testMoveDownWithSourceDelete() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
	
		SimpleTextEdit d1= SimpleTextEdit.createDelete(2, 2);
		d1.add(s1);
		
		RangeMarker m1= new RangeMarker(2, 2);
		s1.add(m1);
		
		fProcessor.add(t1);
		fProcessor.add(d1);
		
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0145623789", fDocument.get());
		assertEquals(d1.getTextRange(), 2, 0);
		assertTrue(s1.isDeleted());
		assertEquals(t1.getTextRange(), 5, 2);
		assertEquals(m1.getTextRange(), 5, 2);
		doUndoRedo(undo, "0145623789");
	}		
	
	public void testMoveUpWithInnerMark() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(7, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		SimpleTextEdit m= SimpleTextEdit.createReplace(4, 1, "yy");
		fProcessor.add(t1);
		fProcessor.add(m);
		fProcessor.add(s1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "017823yy569", fDocument.get());
		assertEquals(s1.getTextRange(), 10, 0);
		assertEquals(t1.getTextRange(), 2, 2);
		assertEquals(m.getTextRange(), 6, 2);
		doUndoRedo(undo, "017823yy569");
	}	
	
	public void testMoveDownWithInnerMark() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		SimpleTextEdit m= SimpleTextEdit.createReplace(4, 1, "yy");
		fProcessor.add(t1);
		fProcessor.add(m);
		fProcessor.add(s1);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "01yy5623789", fDocument.get());
		assertEquals(s1.getTextRange(), 2, 0);
		assertEquals(t1.getTextRange(), 6, 2);
		assertEquals(m.getTextRange(), 2, 2);
		doUndoRedo(undo, "01yy5623789");
	}	
	
	public void testMoveUpWithParentMark() throws Exception {
		RangeMarker m= new RangeMarker(2, 6);
		MoveSourceEdit s1= new MoveSourceEdit(4, 2);
		MoveTargetEdit t1= new MoveTargetEdit(3, s1);
		m.add(s1);
		m.add(t1);
		fProcessor.add(m);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0124536789", fDocument.get());
		assertEquals(m.getTextRange(), 2, 6);
		assertEquals(t1.getTextRange(), 3, 2);
		assertEquals(s1.getTextRange(), 6, 0);		
		doUndoRedo(undo, "0124536789");		
	}
	
	public void testMoveDownWithParentMark() throws Exception {
		RangeMarker m= new RangeMarker(2, 6);
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		m.add(s1);
		m.add(t1);
		fProcessor.add(m);
		assertTrue(fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals("Buffer content", "0142356789", fDocument.get());
		assertEquals(m.getTextRange(), 2, 6);
		assertEquals(t1.getTextRange(), 3, 2);
		assertEquals(s1.getTextRange(), 2, 0);		
		doUndoRedo(undo, "0142356789");		
	}
	
	public void testCopyDown() throws Exception {
		CopySourceEdit s1= new CopySourceEdit(2, 3);
		CopyTargetEdit t1= new CopyTargetEdit(8, s1);
		
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(s1.getTextRange(), 2, 3);
		assertEquals(t1.getTextRange(), 8, 3);
		String result= "0123456723489";
		assertEquals("Buffer content", result, fDocument.get());
		doUndoRedo(undo, result);		
	}
	
	public void testCopyUp() throws Exception {
		CopySourceEdit s1= new CopySourceEdit(7, 2);
		CopyTargetEdit t1= new CopyTargetEdit(3, s1);
		
		fProcessor.add(s1);
		fProcessor.add(t1);
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(s1.getTextRange(), 9, 2);
		assertEquals(t1.getTextRange(), 3, 2);
		String result= "012783456789";
		assertEquals("Buffer content", result, fDocument.get());
		doUndoRedo(undo, result);		
	}
	
	public void testDoubleCopy() throws Exception {
		CopySourceEdit s1= new CopySourceEdit(5, 2);
		CopyTargetEdit t1= new CopyTargetEdit(8, s1);
		CopySourceEdit s2= new CopySourceEdit(5, 2);
		CopyTargetEdit t2= new CopyTargetEdit(2, s2);
		s1.add(s2);
		
		fProcessor.add(s1);
		fProcessor.add(t1);
		fProcessor.add(t2);
		assertTrue("Can perform edits", fProcessor.canPerformEdits());
		UndoMemento undo= fProcessor.performEdits();
		assertEquals(s1.getTextRange(), 7, 2);
		assertEquals(t1.getTextRange(), 10, 2);
		assertEquals(s2.getTextRange(), 7, 2);
		assertEquals(t2.getTextRange(), 2, 2);
		String result= "01562345675689";
		assertEquals("Buffer content", result, fDocument.get());
		doUndoRedo(undo, result);		
	}
	
	public void testSwap1() throws Exception {
		TextBuffer buffer= TextBuffer.create("foo(1, 2), 3");
		TextBufferEditor editor= new TextBufferEditor(buffer);		
		
		MultiTextEdit root= new MultiTextEdit();
		{
			CopySourceEdit innerRoot= new CopySourceEdit(0, 9);
			
			SimpleTextEdit e1= SimpleTextEdit.createReplace(0, 9, "");
			e1.add(innerRoot);
			CopyTargetEdit t1= new CopyTargetEdit(11, innerRoot);
			
			SimpleTextEdit e2= SimpleTextEdit.createReplace(11, 1, "");
			CopySourceEdit s2= new CopySourceEdit(11, 1);
			e2.add(s2);
			CopyTargetEdit t2= new CopyTargetEdit(0, s2);

			root.add(e1);
			root.add(t2);
			root.add(e2);				
			root.add(t1);

			editor.add(root);
		}
		
		assertTrue("Can perform edits", editor.canPerformEdits());
		editor.performEdits();

		String result= "3, foo(1, 2)";
		assertEquals("Buffer content", result, buffer.getContent());
	}
	
	public void testSwap2() throws Exception {
		TextBuffer buffer= TextBuffer.create("foo(1, 2), 3");
		TextBufferEditor editor= new TextBufferEditor(buffer);		
		
		MultiTextEdit innerRoot= new MultiTextEdit();
		{
			SimpleTextEdit e1= SimpleTextEdit.createReplace(4, 1, "");
			CopySourceEdit s1= new CopySourceEdit(4, 1);
			e1.add(s1);
			CopyTargetEdit t1= new CopyTargetEdit(7, s1);
			
			SimpleTextEdit e2= SimpleTextEdit.createReplace(7, 1, "");
			CopySourceEdit s2= new CopySourceEdit(7, 1);
			e2.add(s2);
			CopyTargetEdit t2= new CopyTargetEdit(4, s2);
			
			innerRoot.add(e1);
			innerRoot.add(t2);
			innerRoot.add(e2);				
			innerRoot.add(t1);
		}
		
		editor.add(innerRoot);
		
		assertTrue("Can perform edits", editor.canPerformEdits());
		editor.performEdits();

		String result= "foo(2, 1), 3";
		assertEquals("Buffer content", result, buffer.getContent());
	}	
	
	public void testSwap2InSwap1() throws Exception {
		TextBuffer buffer= TextBuffer.create("foo(1, 2), 3");
		TextBufferEditor editor= new TextBufferEditor(buffer);		
		
		CopySourceEdit innerRoot= new CopySourceEdit(0, 9);
		{
			SimpleTextEdit e1= SimpleTextEdit.createReplace(4, 1, "");
			CopySourceEdit s1= new CopySourceEdit(4, 1);
			e1.add(s1);
			CopyTargetEdit t1= new CopyTargetEdit(7, s1);
			
			SimpleTextEdit e2= SimpleTextEdit.createReplace(7, 1, "");
			CopySourceEdit s2= new CopySourceEdit(7, 1);
			e2.add(s2);
			CopyTargetEdit t2= new CopyTargetEdit(4, s2);
			
			innerRoot.add(e1);
			innerRoot.add(t2);
			innerRoot.add(e2);				
			innerRoot.add(t1);
		}
		MultiTextEdit root= new MultiTextEdit();
		{
			SimpleTextEdit e1= SimpleTextEdit.createReplace(0, 9, "");
			e1.add(innerRoot);
			CopyTargetEdit t1= new CopyTargetEdit(11, innerRoot);
			
			SimpleTextEdit e2= SimpleTextEdit.createReplace(11, 1, "");
			CopySourceEdit s2= new CopySourceEdit(11, 1);
			e2.add(s2);
			CopyTargetEdit t2= new CopyTargetEdit(0, s2);

			root.add(e1);
			root.add(t2);
			root.add(e2);				
			root.add(t1);
			
			editor.add(root);
		}

		assertTrue("Can perform edits", editor.canPerformEdits());
		editor.performEdits();

		String result= "3, foo(2, 1)";
		assertEquals("Buffer content", result, buffer.getContent());
	}	
	
	private void doUndoRedo(UndoMemento undo, String redoResult) throws Exception {
		fProcessor.add(undo);
		UndoMemento redo= fProcessor.performEdits();
		assertBufferContent();
		fProcessor.add(redo);
		undo= fProcessor.performEdits();
		assertEquals("Buffer content redo", redoResult, fDocument.get());
		fProcessor.add(undo);
		fProcessor.performEdits();
		assertBufferContent();
	}
	
	private void assertEquals(TextRange r, int offset, int length) {
		assertEquals("Offset", offset, r.getOffset());
		assertEquals("Length", length, r.getLength());	
	}
	
	private void assertBufferContent() {
		assertEquals("Buffer content restored", "0123456789", fDocument.get());
	}		
}

