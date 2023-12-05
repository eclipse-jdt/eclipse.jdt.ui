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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultLineTracker;
import org.eclipse.jface.text.ILineTracker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextStore;


/**
 *
 * @since 3.2
 */
public abstract class TextStorePerformanceTest extends TextPerformanceTestCase2 {

	protected static final String FAUST1;
	protected static final String FAUST100;
	protected static final String FAUST500;
	protected static final Map<String, String> LOCAL_FINGERPRINTS= new HashMap<>();

	static {
		FAUST1 = AbstractDocumentLineDifferTest.getFaust();
		FAUST100 = FAUST1.substring(0, 100).intern();
		FAUST500 = FAUST1.substring(0, 500).intern();
	}

	abstract protected ITextStore createTextStore();

	private ITextStore fTextStore;

	/*
	 * @see org.eclipse.jdt.text.tests.performance.PerformanceTestCase2#getLocalFingerprints()
	 */
	@Override
	protected final Map<String, String> getLocalFingerprints() {
		return LOCAL_FINGERPRINTS;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		fTextStore = createTextStore();
	}

	public void measureSet(PerformanceMeter meter) {
		meter.start();
		for (int accumulated = 0; accumulated < 400; accumulated++)
			fTextStore.set(FAUST1);
		meter.stop();
	}

	public void measureRepeatedReplace(PerformanceMeter meter) {
		fTextStore.set(FAUST1);
		int OFFSET = 12;
		int REPLACE_LENGTH = FAUST100.length();
		meter.start();
		for (int times = 0; times < 200000; times++)
			fTextStore.replace(OFFSET, REPLACE_LENGTH, FAUST100);
		meter.stop();
	}

	public void measureTypingReplaceInLargeFile(PerformanceMeter meter) {
		fTextStore.set(FAUST1);
		int offset = 12;
		meter.start();
		for (int times = 0; times < 9000000; times++)
			while (offset++ < 400)
				fTextStore.replace(offset, 0, ";");
		meter.stop();
	}

	public void measureTypingReplaceInSmallFile(PerformanceMeter meter) {
		fTextStore.set(FAUST500);
		int offset = 12;
		meter.start();
		for (int times = 0; times < 9000000; times++)
			while (offset++ < 400)
				fTextStore.replace(offset, 0, ";");
		meter.stop();
	}

	public void measureInsertAtStart(PerformanceMeter meter) {
		fTextStore.set(FAUST1);
		meter.start();
		for (int times = 0; times < 1000; times++)
			fTextStore.replace(0, 0, FAUST100);
		meter.stop();
	}

	public void measureInsertAtEnd(PerformanceMeter meter) {
		fTextStore.set("");
		int offset = fTextStore.getLength();
		int length = FAUST100.length();
		meter.start();
		for (int times = 0; times < 3000; times++) {
			fTextStore.replace(offset, 0, FAUST100);
			offset += length;
		}
		meter.stop();
	}

	public void measureDeleteInsert(PerformanceMeter meter) {
		fTextStore.set(FAUST1);
		int OFFSET = 12;
		int REPLACE_LENGTH = 100;
		meter.start();
		for (int times = 0; times < 1000; times++)
			// delete phase
			fTextStore.replace(OFFSET, REPLACE_LENGTH, null);
		for (int times = 0; times < 1000; times++)
			// insert phase
			fTextStore.replace(OFFSET, 0, FAUST100);
		meter.stop();
	}

	public void measureRandomReplace(PerformanceMeter meter) {
		fTextStore.set(FAUST1);

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
				fTextStore.replace(index, REPLACE_LENGTH, null);
				fTextStore.replace(index, 0, replace);
			}
		meter.stop();
	}

	public void measureGetLine(PerformanceMeter meter) throws BadLocationException {
		fTextStore.set(FAUST1);
		ILineTracker tracker= new DefaultLineTracker();
		tracker.set(FAUST1);
		int lines = tracker.getNumberOfLines();
		meter.start();
		for (int times = 0; times < 100; times++) {
			for (int line = 0; line < lines; line++) {
				IRegion lineInfo = tracker.getLineInformation(line);
				fTextStore.get(lineInfo.getOffset(), lineInfo.getLength());
			}
		}
		meter.stop();
	}

	public void measureGetChar(PerformanceMeter meter) {
		fTextStore.set(FAUST1);
		int length = fTextStore.getLength();
		meter.start();
		for (int times = 0; times < 150; times++)
			for (int offset = 0; offset < length; offset++)
				fTextStore.get(offset);
		meter.stop();
	}

	public void measureGetAll(PerformanceMeter meter) {
		fTextStore.set(FAUST1);
		int length = fTextStore.getLength();
		meter.start();
		for (int times = 0; times < 500; times++)
			fTextStore.get(0, length);
		meter.stop();
	}
}
