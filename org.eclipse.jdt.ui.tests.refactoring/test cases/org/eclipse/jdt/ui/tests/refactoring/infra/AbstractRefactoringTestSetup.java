/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.infra;

import java.util.Hashtable;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.eclipse.jdt.testplugin.TestOptions;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;

import org.eclipse.jdt.internal.core.manipulation.CodeTemplateContextType;
import org.eclipse.jdt.internal.core.manipulation.StubUtility;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.util.CoreUtility;


public class AbstractRefactoringTestSetup extends TestSetup {

	private boolean fWasAutobuild;

	public AbstractRefactoringTestSetup(Test test) {
		super(test);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		fWasAutobuild= CoreUtility.setAutoBuilding(false);
		if (JavaPlugin.getActivePage() != null)
			JavaPlugin.getActivePage().close();

		Hashtable<String, String> options= TestOptions.getDefaultOptions();
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.TAB);
		options.put(DefaultCodeFormatterConstants.FORMATTER_NUMBER_OF_EMPTY_LINES_TO_PRESERVE, "0");
		options.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "4");
		options.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, String.valueOf(9999));

		JavaCore.setOptions(options);
		TestOptions.initializeCodeGenerationOptions();
		JavaPlugin.getDefault().getCodeTemplateStore().load();

		String str= """
			/**
			 * ${tags}
			 */""";
		StubUtility.setCodeTemplate(CodeTemplateContextType.CONSTRUCTORCOMMENT_ID, str, null);
	}

	@Override
	protected void tearDown() throws Exception {
		CoreUtility.setAutoBuilding(fWasAutobuild);
		/*
		 * ensure the workbench state gets saved when running with the Automated Testing Framework
         * TODO: remove when https://bugs.eclipse.org/bugs/show_bug.cgi?id=71362 is fixed
         */
		/* Not needed for JDT/UI tests right now.
		StackTraceElement[] elements=  new Throwable().getStackTrace();
		for (int i= 0; i < elements.length; i++) {
			StackTraceElement element= elements[i];
			if (element.getClassName().equals("org.eclipse.test.EclipseTestRunner")) {
				PlatformUI.getWorkbench().close();
				break;
			}
		}
		*/
		super.tearDown();
	}
}
