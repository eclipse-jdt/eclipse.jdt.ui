/*******************************************************************************
 * Copyright (c) 2020 Carsten Hammer and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.eclipse.jdt.internal.corext.fix.ConvertForLoopOperation;
import org.eclipse.jdt.internal.corext.fix.ConvertLoopOperation;

@RunWith(Parameterized.class)
public class ConvertLoopOperationTest extends ConvertForLoopOperation {

	public ConvertLoopOperationTest() {
		super(null);
	}

	@Parameter(value = 0)
	public String name;

	@Parameter(value = 1)
	public String expectedResult;

	@Parameters
	public static Collection<String[]> data() {
		return Arrays.asList(new String[][] {
				{ "children", "Child" },
				{ "xxxxxChildren", "xxxxxChild" },
				{ "proxies", "Proxy" },
				{ "xxxxProxies", "xxxxProxy" },
				{ "dairies ", "element" },
				{ "bus", "element" },
				{ "boxes", "element" },
				{ "lunches", "element" },
				{ "shelves", "element" },
				{ "vetoes", "element" },
				{ "mass", "element" },
				{ "indices", "Index" },
				{ "clazzes", "element" },
				{ "processes", "element" },
				{ "basis", "element" },
				{ "dos", "element" },
				{ "bias", "element" },
				{ "integers", "element" },
				{ "longs", "element" },
				{ "shorts", "element" },
				{ "bytes", "element" },
				{ "booleans", "element" },
				{ "floats", "element" },
				{ "doubles", "element" },
				{ "allAuthors", "Author" },
				{ "all_Users", "User" },
				{ "allChildren", "Child" },
				{ "allProblems", "Problem" },
				{ "alligators", "alligator" },
				{ "allX", "X" },
				{ "allowances", "element" }
		});
	}

	@Test
	public void testModifybasename() {
		Assert.assertEquals(expectedResult, ConvertLoopOperation.modifybasename(name));
	}

}