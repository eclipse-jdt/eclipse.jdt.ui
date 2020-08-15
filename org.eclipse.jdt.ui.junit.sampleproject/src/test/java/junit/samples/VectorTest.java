package junit.samples;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

import junit.framework.*;
import java.util.Vector;

/**
 * A sample test case, testing <code>java.util.Vector</code>.
 *
 */
public class VectorTest extends TestCase {
	protected Vector<?> fEmpty;
	protected Vector<Integer> fFull;

	public static void main(String[] args) {
		junit.textui.TestRunner.run(suite());
	}

	protected void setUp() {
		fEmpty = new Vector<Object>();
		fFull = new Vector<Integer>();
		fFull.addElement(new Integer(1));
		fFull.addElement(new Integer(2));
		fFull.addElement(new Integer(3));
	}

	public static Test suite() {
		return new TestSuite(VectorTest.class);
	}

	public void testCapacity() {
		int size = fFull.size();
		for (int i = 0; i < 100; i++)
			fFull.addElement(new Integer(i));
		assertTrue(fFull.size() == 100 + size);
	}

	public void testClone() {
		Vector<Integer> clone = (Vector<Integer>) fFull.clone();
		assertTrue(clone.size() == fFull.size());
		assertTrue(clone.contains(new Integer(1)));
	}

	public void testContains() {
		assertTrue(fFull.contains(new Integer(1)));
		assertTrue(!fEmpty.contains(new Integer(1)));
	}

	public void testElementAt() {
		Integer i = (Integer) fFull.elementAt(0);
		assertTrue(i.intValue() == 1);

		try {
			fFull.elementAt(fFull.size());
		} catch (ArrayIndexOutOfBoundsException e) {
			return;
		}
		fail("Should raise an ArrayIndexOutOfBoundsException");
	}

	public void testRemoveAll() {
		fFull.removeAllElements();
		fEmpty.removeAllElements();
		assertTrue(fFull.isEmpty());
		assertTrue(fEmpty.isEmpty());
	}

	public void testRemoveElement() {
		fFull.removeElement(new Integer(3));
		assertTrue(!fFull.contains(new Integer(3)));
	}
}
