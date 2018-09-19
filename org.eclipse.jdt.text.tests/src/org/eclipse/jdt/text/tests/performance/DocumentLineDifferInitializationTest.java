/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.test.performance.PerformanceMeter;

import org.eclipse.swt.widgets.Display;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer;

public class DocumentLineDifferInitializationTest extends AbstractDocumentLineDifferTest {
	private static final Class<DocumentLineDifferInitializationTest> THIS= DocumentLineDifferInitializationTest.class;
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	public void testInitializationWithNoChanges() throws Exception {
		setUpFast();
		runInitializationMeasurements(createDocument(FAUST1));
	}

	public void testInitializationWithFewChanges() throws Exception {
		setUpFast();
		runInitializationMeasurements(createDocument(FAUST_FEW_CHANGES));
	}

	public void testInitializationWithManyChanges() throws Exception {
		setUpSlow();
		runInitializationMeasurements(createDocument(SMALL_FAUST_MANY_CHANGES));
	}

	public void testInitializationWithManyChangesButEqualSize() throws Exception {
		setUpSlow();
		runInitializationMeasurements(createDocument(SMALL_FAUST_MANY_CHANGES_SAME_SIZE));
	}

	@Override
	protected void tearDown() throws Exception {
		commitAllMeasurements();
		assertAllPerformance();

		super.tearDown();
	}

	protected IDocument createDocument(String contents) {
		return new Document(contents);
	}

	private void runInitializationMeasurements(IDocument document) throws Exception {
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureInitialization(meter, document);

		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureInitialization(meter, document);
	}

	private void measureInitialization(PerformanceMeter meter, IDocument document) {
		final DocumentLineDiffer differ= new DocumentLineDiffer();
		setUpDiffer(differ);
		DisplayHelper helper= new DisplayHelper() {
			@Override
			public boolean condition() {
				return differ.isSynchronized();
			}
		};
		meter.start();
		differ.connect(document);
		boolean inSync= helper.waitForCondition(Display.getDefault(), MAX_WAIT);
		meter.stop();
		assertTrue(inSync);
		differ.disconnect(document);
	}

}
