/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.text.tests.performance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextStore;


/**
 * A performance test for IDocument implementations.
 *
 * @since 3.3
 */
public abstract class AbstractDocumentPerformanceTest extends TextPerformanceTestCase2 {
	protected static class Doc extends AbstractDocument {
		public Doc(ITextStore store, ILineTracker tracker) {
			setTextStore(store);
			setLineTracker(tracker);
			completeInitialization();
		}
	}

	protected static final String FAUST1;
	protected static final String FAUST100;
	protected static final String FAUST500;
	protected static final Map<String, String> LOCAL_FINGERPRINTS= new HashMap<>();
	protected static final Map<String, String> DEGRADATION_COMMENTS= new HashMap<>();


	static {
		FAUST1 = AbstractDocumentLineDifferTest.getFaust();
		FAUST100 = FAUST1.substring(0, 100).intern();
		FAUST500 = FAUST1.substring(0, 500).intern();

		// Set local fingerprints
		LOCAL_FINGERPRINTS.put("measureDeleteInsert", "Document: delete and insert");
		LOCAL_FINGERPRINTS.put("measureInsertAtStart", "Document: insert at beginning");
		LOCAL_FINGERPRINTS.put("measureInsertAtEnd", "Document: insert at end");
		LOCAL_FINGERPRINTS.put("measureRandomReplace", "Document: random replace");
		LOCAL_FINGERPRINTS.put("measureRepeatedReplace", "Document: repeated replace");
	}


	abstract protected IDocument createDocument();

	private IDocument fDocument;


	/*
	 * @see org.eclipse.jdt.text.tests.performance.PerformanceTestCase2#getLocalFingerprints()
	 */
	@Override
	protected final Map<String, String> getLocalFingerprints() {
		return LOCAL_FINGERPRINTS;
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.PerformanceTestCase2#getDegradations()
	 */
	@Override
	protected Map<String, String> getDegradationComments() {
		return DEGRADATION_COMMENTS;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fDocument = createDocument();
	}

	@Override
	protected void tearDown() throws Exception {
		fDocument= null;
		super.tearDown();
	}

	/* modification */

	public void measureSet(PerformanceMeter meter) {
		meter.start();
		for (int accumulated = 0; accumulated < 30; accumulated++)
			fDocument.set(FAUST1);
		meter.stop();
	}

	public void measureRepeatedReplace(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int OFFSET = 12;
		int REPLACE_LENGTH = FAUST100.length();
		meter.start();
		for (int times = 0; times < 30000; times++)
			fDocument.replace(OFFSET, REPLACE_LENGTH, FAUST100);
		meter.stop();
	}

	public void measureTypingReplaceInLargeFile(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int offset = 12;
		meter.start();
		for (int times = 0; times < 54000000; times++)
			while (offset++ < 400)
				fDocument.replace(offset, 0, ";");
		meter.stop();
	}

	public void measureTypingReplaceInSmallFile(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST500);
		int offset = 12;
		meter.start();
		for (int times = 0; times < 54000000; times++)
			while (offset++ < 400)
				fDocument.replace(offset, 0, ";");
		meter.stop();
	}

	public void measureInsertAtStart(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		meter.start();
		for (int times = 0; times < 3000; times++)
			fDocument.replace(0, 0, FAUST100);
		meter.stop();
	}

	public void measureInsertAtEnd(PerformanceMeter meter) throws BadLocationException {
		fDocument.set("");
		int offset = fDocument.getLength();
		int length = FAUST100.length();
		meter.start();
		for (int times = 0; times < 3000; times++) {
			fDocument.replace(offset, 0, FAUST100);
			offset += length;
		}
		meter.stop();
	}

	public void measureDeleteInsert(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int OFFSET = 12;
		int REPLACE_LENGTH = 100;
		meter.start();
		for (int times = 0; times < 1000; times++)
			// delete phase
			fDocument.replace(OFFSET, REPLACE_LENGTH, null);
		for (int times = 0; times < 1000; times++)
			// insert phase
			fDocument.replace(OFFSET, 0, FAUST100);
		meter.stop();
	}

	public void measureRandomReplace(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);

		Random rand = new Random(132498029834234L);
		int[] indices = new int[1000];
		int REPLACE_LENGTH = 100; // chosen to fit in the gap text store size
		for (int i = 0; i < indices.length; i++)
			indices[i] = rand.nextInt(FAUST1.length() - REPLACE_LENGTH);
		char[] replacement = new char[REPLACE_LENGTH];
		Arrays.fill(replacement, ' ');
		String replace = new String(replacement);

		meter.start();
		for (int times = 0; times < 4; times++)
			for (int index : indices) {
				fDocument.replace(index, REPLACE_LENGTH, null);
				fDocument.replace(index, 0, replace);
			}
		meter.stop();
	}

	/* text store read access */

	public void measureGetChar(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int length = fDocument.getLength();
		meter.start();
		for (int times = 0; times < 8; times++)
			for (int offset = 0; offset < length; offset++)
				fDocument.getChar(offset);
		meter.stop();
	}

//  Commented out: is too fast (0ms)
//	public void measureGetAll(PerformanceMeter meter) throws BadLocationException {
//		fDocument.set(FAUST1);
//		int length = fDocument.getLength();
//		meter.start();
//		for (int times = 0; times < 80; times++)
//			fDocument.get(0, length);
//		meter.stop();
//	}

	/* combined: uses both line tracker and text store */

	public void measureGetLine(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int lines = fDocument.getNumberOfLines();
		meter.start();
		for (int times = 0; times < 30; times++) {
			for (int line = 0; line < lines; line++) {
				IRegion lineInfo = fDocument.getLineInformation(line);
				fDocument.get(lineInfo.getOffset(), lineInfo.getLength());
			}
		}
		meter.stop();
	}

	public void measureGetLength(PerformanceMeter meter) {
		fDocument.set(FAUST1);
		int lines= fDocument.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 500; times++)
			for (int line= 0; line < lines; line++)
				fDocument.getLength();
		meter.stop();
	}

	/* line tracker read access */

	public void measureLineByOffset(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int chars= FAUST1.length();
		meter.start();
		for (int times= 0; times < 2; times++)
			for (int offset= 0; offset <= chars; offset++)
				fDocument.getLineOfOffset(offset);
		meter.stop();
	}

	public void measureLineInfoByOffset(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int chars= FAUST1.length();
		meter.start();
		for (int times= 0; times < 2; times++)
			for (int offset= 0; offset <= chars; offset++)
				fDocument.getLineInformationOfOffset(offset);
		meter.stop();
	}

	public void measureLineByIndex(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int lines= fDocument.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 250; times++)
			for (int line= 0; line < lines; line++)
				fDocument.getLineOffset(line);
		meter.stop();
	}

	public void measureLineInfoByIndex(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int lines= fDocument.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 250; times++)
			for (int line= 0; line < lines; line++)
				fDocument.getLineInformation(line);
		meter.stop();
	}

	public void measureLineLengthByIndex(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int lines= fDocument.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 250; times++)
			for (int line= 0; line < lines; line++)
				fDocument.getLineLength(line);
		meter.stop();
	}

	public void measureLineDelimiterByIndex(PerformanceMeter meter) throws BadLocationException {
		fDocument.set(FAUST1);
		int lines= fDocument.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 250; times++)
			for (int line= 0; line < lines; line++)
				fDocument.getLineDelimiter(line);
		meter.stop();
	}

	public void measureGetNumberOfLines(PerformanceMeter meter) {
		fDocument.set(FAUST1);
		int lines= fDocument.getNumberOfLines();
		meter.start();
		for (int times= 0; times < 250; times++)
			for (int line= 0; line < lines; line++)
				fDocument.getNumberOfLines();
		meter.stop();
	}
}
