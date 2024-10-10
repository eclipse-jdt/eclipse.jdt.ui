/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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
 *     Suite moved from QuickFixTest.java
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix.Java8;

import java.util.Hashtable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;

@Suite
@SelectClasses({
	ExplicitEncodingCleanUpTest.class
})
public class QuickFixJava8TestSuite {
	@BeforeEach
	protected void setUp() throws Exception {
		Hashtable<String, String> defaultOptions= TestOptions.getDefaultOptions();
		defaultOptions.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, Integer.toString(120));
		JavaCore.setOptions(defaultOptions);
		TestOptions.initializeCodeGenerationOptions();
		// Use load since restore doesn't really restore the defaults.
		JavaPlugin.getDefault().getCodeTemplateStore().load();
	}
}
