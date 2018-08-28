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

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.LocationKind;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ISynchronizable;

/**
 *
 * @since 3.2
 */
public class SynchronizedLineDifferInitializationTest extends DocumentLineDifferInitializationTest {
	private static final Class<SynchronizedLineDifferInitializationTest> THIS= SynchronizedLineDifferInitializationTest.class;
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.DocumentLineDifferInitializationText#createDocument(java.lang.String)
	 */
	@Override
	protected IDocument createDocument(String contents) {
		IDocument document= FileBuffers.getTextFileBufferManager().createEmptyDocument(null, LocationKind.IFILE);
		if (document instanceof ISynchronizable)
			((ISynchronizable)document).setLockObject(new Object());
		document.set(contents);
		return document;
	}
}
