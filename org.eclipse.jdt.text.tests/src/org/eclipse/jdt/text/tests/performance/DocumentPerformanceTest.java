/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
	@Override
	protected IDocument createDocument() {
		return new Document();
	}

}
