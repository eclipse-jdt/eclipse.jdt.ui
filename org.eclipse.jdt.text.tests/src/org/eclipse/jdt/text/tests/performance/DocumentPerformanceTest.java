/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;


/**
 * Tests {@link Document} as it comes out of the box.
 * 
 * @since 3.3
 */
public class DocumentPerformanceTest extends AbstractDocumentPerformanceTest {
	public static Test suite() {
		return new PerformanceTestSetup(new PerfTestSuite(DocumentPerformanceTest.class));
	}

	public static Test setUpTest(Test test) {
		return new PerformanceTestSetup(test);
	}


	/*
	 * @see org.eclipse.jdt.text.tests.performance.AbstractDocumentPerformanceTest#createDocument()
	 * @since 3.3
	 */
	protected IDocument createDocument() {
		return new Document();
	}

}
