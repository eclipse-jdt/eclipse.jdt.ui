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

import org.eclipse.text.edits.CopySourceEdit;
import org.eclipse.text.edits.CopyTargetEdit;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MoveTargetEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.RangeMarker;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditCopier;
import org.eclipse.text.edits.UndoMemento;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

public class TextEditTests extends TestCase {

	private static final Class THIS= TextEditTests.class;
	
	private IDocument fDocument;
	private MultiTextEdit fRoot;
	
	public TextEditTests(String name) {
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
		fRoot= new MultiTextEdit(0, fDocument.getLength());
	}
	
	protected void tearDown() throws Exception {
		fRoot= null;
		fRoot= null;
	}
	
	public void testOverlap1() throws Exception {
		// [ [ ] ]
		fRoot.add(new ReplaceEdit(0, 2, "01"));
		boolean exception= false;
		try {
			fRoot.add(new ReplaceEdit(1, 2, "12"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}	
	
	public void testOverlap2() throws Exception {
		// [[ ] ]
		fRoot.add(new ReplaceEdit(0, 2, "01"));
		boolean exception= false;
		try {
			fRoot.add(new ReplaceEdit(0, 1, "0"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}	
	
	public void testOverlap3() throws Exception {
		// [ [ ]]
		fRoot.add(new ReplaceEdit(0, 2, "01"));
		boolean exception= false;
		try {
			fRoot.add(new ReplaceEdit(1, 1, "1"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}	
	
	public void testOverlap4() throws Exception {
		// [ [ ] ]
		fRoot.add(new ReplaceEdit(0, 3, "012"));
		boolean exception= false;
		try {
			fRoot.add(new ReplaceEdit(1, 1, "1"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap5() throws Exception {
		// [ []  ]
		fRoot.add(new ReplaceEdit(0, 3, "012"));
		boolean exception= false;
		try {
			fRoot.add(new InsertEdit(1, "xx"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap6() throws Exception {
		// [  [] ]
		fRoot.add(new ReplaceEdit(0, 3, "012"));
		boolean exception= false;
		try {
			fRoot.add(new InsertEdit(2, "xx"));
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap7() throws Exception {
		MoveSourceEdit source= new MoveSourceEdit(2, 5);
		MoveTargetEdit target= new MoveTargetEdit(3, source);
		fRoot.add(source);
		boolean exception= false;
		try {
			fRoot.add(target);
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testOverlap8() throws Exception {
		MoveSourceEdit source= new MoveSourceEdit(2, 5);
		MoveTargetEdit target= new MoveTargetEdit(6, source);
		fRoot.add(source);
		boolean exception= false;
		try {
			fRoot.add(target);
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
		fRoot.add(s1);
		fRoot.add(t1);
		boolean exception= false;
		try {
			fRoot.add(s2);
			fRoot.add(t2);
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testUnconnected1() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(3, 1);
		boolean exception= false;
		try {
			fRoot.add(s1);
			fRoot.apply(fDocument);
		} catch (MalformedTreeException e) {
			exception= true;
		}
		assertTrue(exception);
	}
	
	public void testCopy1() throws Exception {
		MultiTextEdit root= new MultiTextEdit();
		TextEdit e1= new InsertEdit(2, "yy");
		TextEdit e2= new ReplaceEdit(2, 3, "3456");
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
		TextEdit e1= new InsertEdit(2, "yy");
		TextEdit e2= new ReplaceEdit(2, 3, "3456");
		fRoot.add(e1);
		fRoot.add(e2);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(e1, 2, 2);
		assertEquals(e2, 4, 4);
		assertEquals("Buffer content", "01yy345656789", fDocument.get());
		doUndoRedo(undo, "01yy345656789");
	}
	
	public void testInsert2() throws Exception {
		// [][]
		TextEdit e1= new InsertEdit(2, "yy");
		TextEdit e2= new InsertEdit(2, "xx");
		fRoot.add(e1);
		fRoot.add(e2);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(e1, 2, 2);
		assertEquals(e2, 4, 2);
		assertEquals("Buffer content", "01yyxx23456789", fDocument.get());
		doUndoRedo(undo, "01yyxx23456789");
	}
	
	public void testInsert3() throws Exception {
		// [  ][][  ]
		TextEdit e1= new ReplaceEdit(0, 2, "011");
		TextEdit e2= new InsertEdit(2, "xx");
		TextEdit e3= new ReplaceEdit(2, 2, "2");
		fRoot.add(e1);
		fRoot.add(e2);
		fRoot.add(e3);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(e1, 0, 3);
		assertEquals(e2, 3, 2);
		assertEquals(e3, 5, 1);
		assertEquals("Buffer content", "011xx2456789", fDocument.get());
		doUndoRedo(undo, "011xx2456789");
	}
	
	public void testInsert4() throws Exception {
		TextEdit e1= new InsertEdit(0, "xx");
		fRoot.add(e1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer length", 12, fDocument.getLength());
		assertEquals(e1, 0, 2);
		assertEquals("Buffer content", "xx0123456789", fDocument.get());
		doUndoRedo(undo, "xx0123456789");
	}
	
	public void testInsert5() throws Exception {
		TextEdit e1= new InsertEdit(10, "xx");
		fRoot.add(e1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer length", 12, fDocument.getLength());
		assertEquals(e1, 10, 2);
		assertEquals("Buffer content", "0123456789xx", fDocument.get());
		doUndoRedo(undo, "0123456789xx");
	}
	
	public void testInsertReplace1() throws Exception {
		TextEdit e1= new ReplaceEdit(2, 1, "y");
		TextEdit e2= new InsertEdit(2, "xx");
		fRoot.add(e1);
		fRoot.add(e2);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(e1, 4, 1);
		assertEquals(e2, 2, 2);
		assertEquals("Buffer content", "01xxy3456789", fDocument.get());
		doUndoRedo(undo, "01xxy3456789");
	}
	
	public void testDelete1() throws Exception {
		TextEdit e1= new DeleteEdit(3, 1);
		fRoot.add(e1);
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(e1, 3, 0);
		assertEquals("Buffer content", "012456789", fDocument.get());
		doUndoRedo(undo, "012456789");
	}
	
	public void testDelete2() throws Exception {
		TextEdit e1= new DeleteEdit(4, 1);
		TextEdit e2= new DeleteEdit(3, 1);
		TextEdit e3= new DeleteEdit(5, 1);
		fRoot.add(e1);
		fRoot.add(e2);
		fRoot.add(e3);
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(e1, 3, 0);
		assertEquals(e2, 3, 0);
		assertEquals(e3, 3, 0);
		assertEquals("Buffer content", "0126789", fDocument.get());
		doUndoRedo(undo, "0126789");
	}
	
	public void testDelete3() throws Exception {
		TextEdit e1= new InsertEdit(3, "x");
		TextEdit e2= new DeleteEdit(3, 1);
		fRoot.add(e1);
		fRoot.add(e2);
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(e1, 3, 1);
		assertEquals(e2, 4, 0);
		assertEquals("Buffer content", "012x456789", fDocument.get());
		doUndoRedo(undo, "012x456789");
	}
	
	public void testDeleteWithChildren() throws Exception {
		TextEdit e1= new DeleteEdit(2, 6);
		MultiTextEdit e2= new MultiTextEdit(3, 3);
		e1.add(e2);
		TextEdit e3= new ReplaceEdit(3, 1, "xx");
		TextEdit e4= new ReplaceEdit(5, 1, "yy");
		e2.add(e3);
		e2.add(e4);
		fRoot.add(e1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0189", fDocument.get());
		assertTrue(e2.isDeleted());
		assertTrue(e3.isDeleted());
		assertTrue(e4.isDeleted());
		doUndoRedo(undo, "0189");
	}
	
	public void testMove1() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0142356789", fDocument.get());
		assertEquals(s1, 2, 0);
		assertEquals(t1, 3, 2);
		doUndoRedo(undo, "0142356789");
	}
	
	public void testMove2() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(5, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0156234789", fDocument.get());
		assertEquals(s1, 7, 0);
		assertEquals(t1, 2, 2);
		doUndoRedo(undo, "0156234789");
	}

	public void testMove3() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		TextEdit e2= new ReplaceEdit(4, 1, "x");
		fRoot.add(s1);
		fRoot.add(t1);
		fRoot.add(e2);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "01x5623789", fDocument.get());
		assertEquals(s1, 2, 0);
		assertEquals(t1, 5, 2);
		assertEquals(e2, 2, 1);
		doUndoRedo(undo, "01x5623789");
	}
	
	public void testMove4() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(7, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		TextEdit e2= new ReplaceEdit(5, 1, "x");
		fRoot.add(s1);
		fRoot.add(t1);
		fRoot.add(e2);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0178234x69", fDocument.get());
		assertEquals(s1, 9, 0);
		assertEquals(t1, 2, 2);
		assertEquals(e2, 7, 1);
		doUndoRedo(undo, "0178234x69");
	}
	
	public void testMove5() throws Exception {
		// Move onto itself
		MoveSourceEdit s1= new MoveSourceEdit(2, 1);
		MoveTargetEdit t1= new MoveTargetEdit(3, s1);
		TextEdit e2= new ReplaceEdit(2, 1, "x");
		s1.add(e2);
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(s1, 2, 0);
		assertEquals(t1, 2, 1);
		assertEquals(e2, 2, 1);
		assertEquals("Buffer content", "01x3456789", fDocument.get());
		doUndoRedo(undo, "01x3456789");
	}
	
	public void testMove6() throws Exception {
		// Move onto itself
		MoveSourceEdit s1= new MoveSourceEdit(2, 1);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		TextEdit e2= new ReplaceEdit(2, 1, "x");
		s1.add(e2);
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(s1, 3, 0);
		assertEquals(t1, 2, 1);
		assertEquals(e2, 2, 1);
		assertEquals("Buffer content", "01x3456789", fDocument.get());
		doUndoRedo(undo,"01x3456789");
	}
	
	public void testMove7() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 3);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		TextEdit e2= new ReplaceEdit(3, 1, "x");
		s1.add(e2);
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "01562x4789", fDocument.get());
		assertEquals(s1, 2, 0);
		assertEquals(t1, 4, 3);
		assertEquals(e2, 5, 1);
		doUndoRedo(undo, "01562x4789");
	}
	
	public void testMove8() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(5, 3);
		MoveTargetEdit t1= new MoveTargetEdit(1, s1);
		TextEdit e2= new ReplaceEdit(6, 1, "x");
		s1.add(e2);
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "05x7123489", fDocument.get());
		assertEquals(s1, 8, 0);
		assertEquals(t1, 1, 3);
		assertEquals(e2, 2, 1);
		doUndoRedo(undo, "05x7123489");
	}
		
	public void testMove9() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(1, 3);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		
		MoveSourceEdit s2= new MoveSourceEdit(1, 1);
		MoveTargetEdit t2= new MoveTargetEdit(3, s2);
		s1.add(s2);
		s1.add(t2);
		
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(s1, 1, 0);
		assertEquals(t1, 2, 3);
		
		assertEquals(s2, 2, 0);
		assertEquals(t2, 3, 1);
		assertEquals("Buffer content", "0421356789", fDocument.get());
		doUndoRedo(undo, "0421356789");
	}
	
	public void testMove10() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(8, s1);
		MoveSourceEdit s2= new MoveSourceEdit(5, 2);
		MoveTargetEdit t2= new MoveTargetEdit(1, s2);
		
		fRoot.add(s1);
		fRoot.add(t1);
		fRoot.add(s2);
		fRoot.add(t2);
		
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(s1, 4, 0);
		assertEquals(t1, 6, 2);		
		assertEquals(s2, 5, 0);
		assertEquals(t2, 1, 2);
		assertEquals("Buffer content", "0561472389", fDocument.get());
		doUndoRedo(undo, "0561472389");		
	}
	
	public void testMoveWithRangeMarker() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		
		RangeMarker marker= new RangeMarker(2, 2);
		s1.add(marker);		
		
		fRoot.add(s1);
		fRoot.add(t1);
		
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0142356789", fDocument.get());
		assertEquals(s1, 2, 0);
		assertEquals(t1, 3, 2);
		assertEquals(marker, 3, 2);
		doUndoRedo(undo, "0142356789");
	}
	
	public void testMoveWithTargetDelete() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 3);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		TextEdit e2= new DeleteEdit(6, 2);
		e2.add(t1);
		fRoot.add(s1);
		fRoot.add(e2);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "01589", fDocument.get());
		assertEquals(s1, 2, 0);
		assertTrue(t1.isDeleted());
		assertEquals(e2, 3, 0);
		doUndoRedo(undo, "01589");
	}
	
	public void testMoveUpWithSourceDelete() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(5, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		
		TextEdit d1= new DeleteEdit(5, 2);
		d1.add(s1);
		
		RangeMarker marker= new RangeMarker(5, 2);
		s1.add(marker);		
		
		fRoot.add(d1);
		fRoot.add(t1);
		
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0156234789", fDocument.get());
		assertEquals(t1, 2, 2);
		assertEquals(marker, 2, 2);
		assertTrue(s1.isDeleted());
		assertEquals(d1, 7, 0);
		doUndoRedo(undo, "0156234789");
	}		
	
	public void testMoveDown() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		TextEdit i1= new InsertEdit(5, "x");
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		TextEdit d1= new DeleteEdit(9, 1);
	
		RangeMarker m1= new RangeMarker(2, 2);
		s1.add(m1);
		
		fRoot.add(s1);
		fRoot.add(i1);
		fRoot.add(t1);
		fRoot.add(d1);
		
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "014x562378", fDocument.get());
		assertEquals(s1, 2, 0);
		assertEquals(i1, 3, 1);
		assertEquals(t1, 6, 2);
		assertEquals(m1, 6, 2);
		assertEquals(d1, 10, 0);
		doUndoRedo(undo, "014x562378");
	}		
		
	public void testMoveUp() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(7, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		TextEdit i1= new InsertEdit(5, "x");
		TextEdit d1= new DeleteEdit(9, 1);
	
		RangeMarker m1= new RangeMarker(7, 2);
		s1.add(m1);
		
		fRoot.add(s1);
		fRoot.add(i1);
		fRoot.add(t1);
		fRoot.add(d1);
		
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0178234x56", fDocument.get());
		assertEquals(s1, 10, 0);
		assertEquals(i1, 7, 1);
		assertEquals(t1, 2, 2);
		assertEquals(m1, 2, 2);
		assertEquals(d1, 10, 0);
		doUndoRedo(undo, "0178234x56");
	}		
		
	public void testMoveDownWithSourceDelete() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
	
		TextEdit d1= new DeleteEdit(2, 2);
		d1.add(s1);
		
		RangeMarker m1= new RangeMarker(2, 2);
		s1.add(m1);
		
		fRoot.add(t1);
		fRoot.add(d1);
		
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0145623789", fDocument.get());
		assertEquals(d1, 2, 0);
		assertTrue(s1.isDeleted());
		assertEquals(t1, 5, 2);
		assertEquals(m1, 5, 2);
		doUndoRedo(undo, "0145623789");
	}		
	
	public void testMoveUpWithInnerMark() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(7, 2);
		MoveTargetEdit t1= new MoveTargetEdit(2, s1);
		TextEdit m= new ReplaceEdit(4, 1, "yy");
		fRoot.add(t1);
		fRoot.add(m);
		fRoot.add(s1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "017823yy569", fDocument.get());
		assertEquals(s1, 10, 0);
		assertEquals(t1, 2, 2);
		assertEquals(m, 6, 2);
		doUndoRedo(undo, "017823yy569");
	}	
	
	public void testMoveDownWithInnerMark() throws Exception {
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(7, s1);
		TextEdit m= new ReplaceEdit(4, 1, "yy");
		fRoot.add(t1);
		fRoot.add(m);
		fRoot.add(s1);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "01yy5623789", fDocument.get());
		assertEquals(s1, 2, 0);
		assertEquals(t1, 6, 2);
		assertEquals(m, 2, 2);
		doUndoRedo(undo, "01yy5623789");
	}	
	
	public void testMoveUpWithParentMark() throws Exception {
		RangeMarker m= new RangeMarker(2, 6);
		MoveSourceEdit s1= new MoveSourceEdit(4, 2);
		MoveTargetEdit t1= new MoveTargetEdit(3, s1);
		m.add(s1);
		m.add(t1);
		fRoot.add(m);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0124536789", fDocument.get());
		assertEquals(m, 2, 6);
		assertEquals(t1, 3, 2);
		assertEquals(s1, 6, 0);		
		doUndoRedo(undo, "0124536789");		
	}
	
	public void testMoveDownWithParentMark() throws Exception {
		RangeMarker m= new RangeMarker(2, 6);
		MoveSourceEdit s1= new MoveSourceEdit(2, 2);
		MoveTargetEdit t1= new MoveTargetEdit(5, s1);
		m.add(s1);
		m.add(t1);
		fRoot.add(m);
		assertTrue(fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals("Buffer content", "0142356789", fDocument.get());
		assertEquals(m, 2, 6);
		assertEquals(t1, 3, 2);
		assertEquals(s1, 2, 0);		
		doUndoRedo(undo, "0142356789");		
	}
	
	public void testCopyDown() throws Exception {
		CopySourceEdit s1= new CopySourceEdit(2, 3);
		CopyTargetEdit t1= new CopyTargetEdit(8, s1);
		
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(s1, 2, 3);
		assertEquals(t1, 8, 3);
		String result= "0123456723489";
		assertEquals("Buffer content", result, fDocument.get());
		doUndoRedo(undo, result);		
	}
	
	public void testCopyUp() throws Exception {
		CopySourceEdit s1= new CopySourceEdit(7, 2);
		CopyTargetEdit t1= new CopyTargetEdit(3, s1);
		
		fRoot.add(s1);
		fRoot.add(t1);
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(s1, 9, 2);
		assertEquals(t1, 3, 2);
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
		
		fRoot.add(s1);
		fRoot.add(t1);
		fRoot.add(t2);
		assertTrue("Can perform edits", fRoot.canApply(fDocument));
		UndoMemento undo= fRoot.apply(fDocument);
		assertEquals(s1, 7, 2);
		assertEquals(t1, 10, 2);
		assertEquals(s2, 7, 2);
		assertEquals(t2, 2, 2);
		String result= "01562345675689";
		assertEquals("Buffer content", result, fDocument.get());
		doUndoRedo(undo, result);		
	}
	
	public void testSwap1() throws Exception {
		IDocument document= new Document("foo(1, 2), 3");
		
		MultiTextEdit root= new MultiTextEdit();
		{
			CopySourceEdit innerRoot= new CopySourceEdit(0, 9);
			
			TextEdit e1= new ReplaceEdit(0, 9, "");
			e1.add(innerRoot);
			CopyTargetEdit t1= new CopyTargetEdit(11, innerRoot);
			
			TextEdit e2= new ReplaceEdit(11, 1, "");
			CopySourceEdit s2= new CopySourceEdit(11, 1);
			e2.add(s2);
			CopyTargetEdit t2= new CopyTargetEdit(0, s2);

			root.add(e1);
			root.add(t2);
			root.add(e2);				
			root.add(t1);
		}
		
		assertTrue("Can perform edits", root.canApply(document));
		root.apply(document);

		String result= "3, foo(1, 2)";
		assertEquals("Buffer content", result, document.get());
	}
	
	public void testSwap2() throws Exception {
		IDocument document= new Document("foo(1, 2), 3");
		
		MultiTextEdit root= new MultiTextEdit();
		{
			TextEdit e1= new ReplaceEdit(4, 1, "");
			CopySourceEdit s1= new CopySourceEdit(4, 1);
			e1.add(s1);
			CopyTargetEdit t1= new CopyTargetEdit(7, s1);
			
			TextEdit e2= new ReplaceEdit(7, 1, "");
			CopySourceEdit s2= new CopySourceEdit(7, 1);
			e2.add(s2);
			CopyTargetEdit t2= new CopyTargetEdit(4, s2);
			
			root.add(e1);
			root.add(t2);
			root.add(e2);				
			root.add(t1);
		}
		
		assertTrue("Can perform edits", root.canApply(document));
		root.apply(document);

		String result= "foo(2, 1), 3";
		assertEquals("Buffer content", result, document.get());
	}	
	
	public void testSwap2InSwap1() throws Exception {
		IDocument document= new Document("foo(1, 2), 3");
		
		CopySourceEdit innerRoot= new CopySourceEdit(0, 9);
		{
			TextEdit e1= new ReplaceEdit(4, 1, "");
			CopySourceEdit s1= new CopySourceEdit(4, 1);
			e1.add(s1);
			CopyTargetEdit t1= new CopyTargetEdit(7, s1);
			
			TextEdit e2= new ReplaceEdit(7, 1, "");
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
			TextEdit e1= new ReplaceEdit(0, 9, "");
			e1.add(innerRoot);
			CopyTargetEdit t1= new CopyTargetEdit(11, innerRoot);
			
			TextEdit e2= new ReplaceEdit(11, 1, "");
			CopySourceEdit s2= new CopySourceEdit(11, 1);
			e2.add(s2);
			CopyTargetEdit t2= new CopyTargetEdit(0, s2);

			root.add(e1);
			root.add(t2);
			root.add(e2);				
			root.add(t1);
		}

		assertTrue("Can perform edits", root.canApply(document));
		root.apply(document);

		String result= "3, foo(2, 1)";
		assertEquals("Buffer content", result, document.get());
	}	
	
	private void doUndoRedo(UndoMemento undo, String redoResult) throws Exception {
		UndoMemento redo= undo.apply(fDocument);
		assertBufferContent();
		undo= redo.apply(fDocument);
		assertEquals("Buffer content redo", redoResult, fDocument.get());
		undo.apply(fDocument);
		assertBufferContent();
	}
	
	private void assertEquals(TextEdit edit, int offset, int length) {
		assertEquals("Offset", offset, edit.getOffset());
		assertEquals("Length", length, edit.getLength());	
	}
	
	private void assertBufferContent() {
		assertEquals("Buffer content restored", "0123456789", fDocument.get());
	}		
}

