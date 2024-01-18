/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.leaks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultUndoManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextViewer;

import org.eclipse.jdt.ui.leaktest.LeakTestCase;
import org.eclipse.jdt.ui.tests.core.rules.LeakTestSetup;

/**
 * Test for leaks in DefaultUndoManager.
 *
 * @since 3.1
 */
public class UndoManagerLeakTest extends LeakTestCase {

	@Rule
	public LeakTestSetup projectSetup = new LeakTestSetup();

	/** The maximum undo level. */
	private static final int MAX_UNDO_LEVEL= 256;

	/** The shell. */
	private Shell fShell;
	/** The text viewer. */
	private ITextViewer fTextViewer;
	/** The undo manager. */
	private IUndoManager fUndoManager;

	/*
	 *  @see TestCase#setUp()
	 */
	@Override
	@Before
	public void setUp() {
		fShell= new Shell();
		fUndoManager= new DefaultUndoManager(MAX_UNDO_LEVEL);
		fTextViewer= new TextViewer(fShell, SWT.NONE);
		fTextViewer.setUndoManager(fUndoManager);
		fUndoManager.connect(fTextViewer);
	}

	@Test
	public void testUndoManagerLeak() {
		internalTestConvertLineDelimiters();
		internalTestRandomAccess();
		internalTestRandomAccessAsCompound();

		fUndoManager.disconnect();
		fUndoManager= null;

		fShell.dispose();
		fShell= null;

		fTextViewer= null;


		String[] types= {
			TextViewer.class.getName(),
			DefaultUndoManager.class.getName(),

			getDefaultUndoManagersInnerClass("HistoryListener").getName(),
			getDefaultUndoManagersInnerClass("TextCommand").getName(),
			getDefaultUndoManagersInnerClass("CompoundTextCommand").getName(),
		};
		int expected[]= new int[] { 0, 0, 0, 0, 0};

		assertInstanceCount(types, expected);
	}

	private Class<?> getDefaultUndoManagersInnerClass(String className) {
		try {
			return Class.forName("org.eclipse.jface.text.DefaultUndoManager$" + className, true, getClass().getClassLoader());
		} catch (ClassNotFoundException e) {
			fail();
			return null;
		}
	}

	/**
	 * Test for line delimiter conversion.
	 */
	private void internalTestConvertLineDelimiters() {
		final String original= "a\r\nb\r\n";
		final IDocument document= new Document(original);
		fTextViewer.setDocument(document);

		try {
			document.replace(1, 2, "\n");
			document.replace(3, 2, "\n");
		} catch (BadLocationException e) {
			fail();
		}

		assertTrue(fUndoManager.undoable());
		fUndoManager.undo();
		assertTrue(fUndoManager.undoable());
		fUndoManager.undo();

		final String reverted= document.get();

		assertEquals(original, reverted);
	}

	/**
	 * Randomly applies document changes.
	 */
	private void internalTestRandomAccess() {
		final int RANDOM_STRING_LENGTH= 50;
		final int RANDOM_REPLACE_COUNT= 100;

		assertTrue(RANDOM_REPLACE_COUNT >= 1);
		assertTrue(RANDOM_REPLACE_COUNT <= MAX_UNDO_LEVEL);

		String original= createRandomString(RANDOM_STRING_LENGTH);
		final IDocument document= new Document(original);
		fTextViewer.setDocument(document);

		doChange(document, RANDOM_REPLACE_COUNT);

		assertTrue(fUndoManager.undoable());
		while (fUndoManager.undoable())
			fUndoManager.undo();

		final String reverted= document.get();

		assertEquals(original, reverted);
	}

	private void doChange(IDocument document, int count) {
		try {
			for (int i= 0; i < count; i++) {
				final Position position= createRandomPositionPoisson(document.getLength());
				final String string= createRandomStringPoisson();
				document.replace(position.getOffset(), position.getLength(), string);
			}
		} catch (BadLocationException e) {
			fail();
		}
	}

	private void internalTestRandomAccessAsCompound() {
		final int RANDOM_STRING_LENGTH= 50;
		final int RANDOM_REPLACE_COUNT= 100;

		assertTrue(RANDOM_REPLACE_COUNT >= 1);
		assertTrue(RANDOM_REPLACE_COUNT <= MAX_UNDO_LEVEL);

		String original= createRandomString(RANDOM_STRING_LENGTH);
		final IDocument document= new Document(original);
		fTextViewer.setDocument(document);

		fUndoManager.beginCompoundChange();
		doChange(document, RANDOM_REPLACE_COUNT);
		fUndoManager.endCompoundChange();

		assertTrue(fUndoManager.undoable());
		while (fUndoManager.undoable())
			fUndoManager.undo();
		assertFalse(fUndoManager.undoable());

		final String reverted= document.get();

		assertEquals(original, reverted);
	}

	private static String createRandomString(int length) {
		final StringBuilder buffer= new StringBuilder();

		for (int i= 0; i < length; i++)
			buffer.append(getRandomCharacter());

		return buffer.toString();
	}

	private static final char getRandomCharacter() {
//		return Math.random() < 0.5
//			? '\r'
//			: '\n';

		// XXX must include \r, \n, \t
		return (char) (32 + 95 * Math.random());
	}

	private static String createRandomStringPoisson() {
		final int length= getRandomPoissonValue(2);
		return createRandomString(length);
	}

	private static Position createRandomPositionPoisson(int documentLength) {

		final float random= (float) Math.random();
		final int offset= (int) (random * (documentLength + 1));

		int length= getRandomPoissonValue(2);
		if (offset + length > documentLength)
			length= documentLength - offset;

		return new Position(offset, length);
	}

	private static int getRandomPoissonValue(int mean) {
		final int MAX_VALUE= 10;

		final float random= (float) Math.random();
		float probability= 0;
		int i= 0;
		while (probability < 1 && i < MAX_VALUE) {
			probability += getPoissonDistribution(mean, i);
			if (random <= probability)
				break;
			i++;
		}
		return i;
	}

	private static float getPoissonDistribution(float lambda, int k) {
		return (float) (Math.exp(-lambda) * Math.pow(lambda, k) / faculty(k));
	}

	/**
	 * Returns the faculty of k.
	 */
	private static final int faculty(int k) {
		return k == 0
			? 1
			: k * faculty(k - 1);
	}

}
