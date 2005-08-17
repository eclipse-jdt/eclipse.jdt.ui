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

import org.eclipse.jface.text.IDocument;

import org.eclipse.jdt.internal.ui.javaeditor.PartiallySynchronizedDocument;

/**
 * 
 * @since 3.2
 */
public class SynchronizedLineDifferInitializationTest extends DocumentLineDifferInitializationTest {
	private static final Class THIS= SynchronizedLineDifferInitializationTest.class;
	public static Test suite() {
		return new PerformanceTestSetup(new TestSuite(THIS));
	}

	/*
	 * @see org.eclipse.jdt.text.tests.performance.DocumentLineDifferInitializationText#createDocument(java.lang.String)
	 */
	protected IDocument createDocument(String contents) {
		PartiallySynchronizedDocument document= new PartiallySynchronizedDocument();
		document.set(contents);
		return document;
	}
}
