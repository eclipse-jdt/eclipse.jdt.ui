/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class TypeTest {

	@Test
	public void testTypeTuplesuniqueHashcode() {
		TypeEnvironment te=new TypeEnvironment();
		assertNotEquals("HashCode of two different TypeTuples should be different",
				new TypeTuple(te.INT, te.BOOLEAN).hashCode(),
				new TypeTuple(te.INT, te.DOUBLE).hashCode());
	}
}
