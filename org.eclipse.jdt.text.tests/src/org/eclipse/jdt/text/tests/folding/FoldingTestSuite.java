/*******************************************************************************
 * Copyright (c) 2025, 2026 Daniel Schmid and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Daniel Schmid - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.folding;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
	FoldingTest.class,
	MarkdownJavadocFoldingTest.class,
	CustomFoldingRegionTest.class,
	FoldingWithShowSelectedElementTests.class,
	FoldingIncludeClosingBracketTests.class
})
public class FoldingTestSuite {
}
