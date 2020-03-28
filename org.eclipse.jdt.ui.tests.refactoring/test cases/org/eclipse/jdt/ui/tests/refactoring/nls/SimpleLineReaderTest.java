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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.eclipse.jface.text.Document;

import org.eclipse.jdt.internal.corext.refactoring.nls.SimpleLineReader;

public class SimpleLineReaderTest {
	@Test
	public void simpleLineReader() throws Exception {
        SimpleLineReader reader = new SimpleLineReader(new Document("aha\noho\r\n\r\n\n"));
        assertEquals("aha\n", reader.readLine());
        assertEquals("oho\r\n", reader.readLine());
        assertEquals("\r\n", reader.readLine());
        assertEquals("\n", reader.readLine());
        assertNull(reader.readLine());
    }

	@Test
	public void simpleLineReaderWithEmptyString() {
        SimpleLineReader simpleLineReader = new SimpleLineReader(new Document(""));
        assertNull(simpleLineReader.readLine());
    }

	@Test
	public void simpleLineReaderWithEscapedLF() {
        SimpleLineReader simpleLineReader = new SimpleLineReader(new Document("a\nb\\nc\n"));
        assertEquals("a\n", simpleLineReader.readLine());
        assertEquals("b\\nc\n", simpleLineReader.readLine());
        assertNull(simpleLineReader.readLine());
    }

	@Test
	public void simpleLineReaderWithEscapedCR() {
        SimpleLineReader simpleLineReader = new SimpleLineReader(new Document("a\nb\\rc\r"));
        assertEquals("a\n", simpleLineReader.readLine());
        assertEquals("b\\rc\r", simpleLineReader.readLine());
        assertNull(simpleLineReader.readLine());
    }

	@Test
	public void simpleLineReaderWithCR() {
        SimpleLineReader simpleLineReader = new SimpleLineReader(new Document("a\rb\r"));
        assertEquals("a\r", simpleLineReader.readLine());
        assertEquals("b\r", simpleLineReader.readLine());
        assertNull(simpleLineReader.readLine());
    }

	@Test
	public void simpleLineReaderWithoutNL() {
        SimpleLineReader simpleLineReader = new SimpleLineReader(new Document("="));
        assertEquals("=", simpleLineReader.readLine());
        assertNull(simpleLineReader.readLine());
    }

	@Test
	public void simpleLineReaderWithMissingNL() {
        SimpleLineReader simpleLineReader = new SimpleLineReader(new Document("a\rb"));
        assertEquals("a\r", simpleLineReader.readLine());
        assertEquals("b", simpleLineReader.readLine());
        assertNull(simpleLineReader.readLine());
    }

	@Test
	public void simpleLineReaderWithLineContinue1() {
        SimpleLineReader simpleLineReader = new SimpleLineReader(new Document("aaa\\\nbbb\nccc\n"));
        assertEquals("aaa\\\nbbb\n", simpleLineReader.readLine());
        assertEquals("ccc\n", simpleLineReader.readLine());
        assertNull(simpleLineReader.readLine());
    }

}
