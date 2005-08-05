/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.jface.text.Document;

import org.eclipse.test.performance.PerformanceMeter;
import org.eclipse.ui.internal.texteditor.quickdiff.DocumentLineDiffer;

public class DocumentLineDifferInitializationText extends AbstractDocumentLineDifferTest {
	private static final Class THIS= DocumentLineDifferInitializationText.class;
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}


	public void testInitializationWithNoChanges() throws Exception {
		setUpFast();
		runInitializationMeasurements(new Document(FAUST1));
	}

	public void testInitializationWithFewChanges() throws Exception {
		setUpFast();
		runInitializationMeasurements(new Document(FAUST_FEW_CHANGES));
	}

	public void testInitializationWithManyChanges() throws Exception {
		setUpSlow();
		runInitializationMeasurements(new Document(SMALL_FAUST_MANY_CHANGES));
	}

	public void testInitializationWithManyChangesButEqualSize() throws Exception {
		setUpSlow();
		runInitializationMeasurements(new Document(SMALL_FAUST_MANY_CHANGES_SAME_SIZE));
	}
	
	protected void tearDown() throws Exception {
		commitAllMeasurements();
		assertAllPerformance();
		
		super.tearDown();
	}

	private void runInitializationMeasurements(Document document) throws Exception {
		PerformanceMeter meter= getNullPerformanceMeter();
		int runs= getWarmUpRuns();
		for (int run= 0; run < runs; run++)
			measureInitialization(meter, document);
		
		meter= createPerformanceMeter();
		runs= getMeasuredRuns();
		for (int run= 0; run < runs; run++)
			measureInitialization(meter, document);
	}

	private void measureInitialization(PerformanceMeter meter, Document document) throws InterruptedException {
		DocumentLineDiffer differ= new DocumentLineDiffer();
		setUpDiffer(differ);
		BooleanFuture future= waitForSynchronization(differ);
		meter.start();
		differ.connect(document);
		boolean inSync= future.get();
		meter.stop();
		assertTrue(inSync);
		future.cancel();
		differ.disconnect(document);
	}

}
