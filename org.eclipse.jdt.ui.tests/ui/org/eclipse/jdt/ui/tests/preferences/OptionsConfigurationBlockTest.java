/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.preferences;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.preferences.ComplianceConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.JavaBuildConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.JavadocProblemsConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.NameConventionConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.ProblemSeveritiesConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.TodoTaskConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key;

public class OptionsConfigurationBlockTest extends TestCase {

	/*
	 * NOTE: This test is not contained in the build test suite, since missing
	 * UI should not make the build go red.
	 */
	public static Test suite() {
		return new TestSuite(OptionsConfigurationBlockTest.class);
	}

	/**
	 * Reflective test that ensures that all options from {@link JavaCore} are used in the UI.
	 *
	 * @throws Exception should not
	 */
	public void testKeysForOptions() throws Exception {
		Field[] coreFields= JavaCore.class.getDeclaredFields();
		HashMap coreFieldLookup= new HashMap();
		for (int i= 0; i < coreFields.length; i++) {
			Field field= coreFields[i];
			String name= field.getName();
			if (name.startsWith("COMPILER_")
					|| name.startsWith("CORE_")
					|| name.startsWith("CODEASSIST_")
					|| name.startsWith("TIMEOUT_")
					) {
				field.setAccessible(true);
				String value= (String) field.get(null);
				if (value.startsWith(JavaCore.PLUGIN_ID))
					coreFieldLookup.put(value, name);
			}
		}

		// default visible classes:
		Class codeAssistAdvancedConfigurationBlock= Class.forName("org.eclipse.jdt.internal.ui.preferences.CodeAssistAdvancedConfigurationBlock");
		checkConfigurationBlock(codeAssistAdvancedConfigurationBlock, coreFieldLookup);
		Class codeAssistConfigurationBlock= Class.forName("org.eclipse.jdt.internal.ui.preferences.CodeAssistConfigurationBlock");
		checkConfigurationBlock(codeAssistConfigurationBlock, coreFieldLookup);

		checkConfigurationBlock(ComplianceConfigurationBlock.class, coreFieldLookup);
		checkConfigurationBlock(JavaBuildConfigurationBlock.class, coreFieldLookup);
		checkConfigurationBlock(JavadocProblemsConfigurationBlock.class, coreFieldLookup);
		checkConfigurationBlock(NameConventionConfigurationBlock.class, coreFieldLookup);
		checkConfigurationBlock(ProblemSeveritiesConfigurationBlock.class, coreFieldLookup);
		checkConfigurationBlock(TodoTaskConfigurationBlock.class, coreFieldLookup);

		removeUnusedOptions(coreFieldLookup);

		assertEquals("Core constants missing in the UI",
				Collections.EMPTY_MAP.toString(),
				coreFieldLookup.toString().replace(',', '\n'));
	}

	/**
	 * @deprecated to hide deprecation warnings
	 * @param coreFieldLookup lookup
	 */
	private void removeUnusedOptions(HashMap coreFieldLookup) {
		coreFieldLookup.keySet().removeAll(Arrays.asList(new String[] {
				JavaCore.COMPILER_PB_INCONSISTENT_NULL_CHECK,
				JavaCore.COMPILER_PB_INVALID_IMPORT,
				JavaCore.COMPILER_PB_UNREACHABLE_CODE,
				JavaCore.COMPILER_PB_UNSAFE_TYPE_OPERATION,
				JavaCore.COMPILER_PB_BOOLEAN_METHOD_THROWING_EXCEPTION,

				JavaCore.CODEASSIST_VISIBILITY_CHECK, // gets set directly, based on org.eclipse.jdt.ui.PreferenceConstants.CODEASSIST_SHOW_VISIBLE_PROPOSALS

				JavaCore.CORE_ENCODING, // useless copy of option from ResourcesPlugin
				JavaCore.CORE_JAVA_BUILD_ORDER, // not relevant any more, since project references are not necessary for build path

				JavaCore.COMPILER_PB_OVERRIDING_METHOD_WITHOUT_SUPER_INVOCATION, // not useful without suppressWarnings for 1.4: https://bugs.eclipse.org/bugs/show_bug.cgi?id=156736
				JavaCore.COMPILER_PB_UNUSED_TYPE_ARGUMENTS_FOR_METHOD_INVOCATION, // maybe for 1.7

				JavaCore.CODEASSIST_IMPLICIT_QUALIFICATION, // TODO: not used: bug?
				
				JavaCore.COMPILER_PB_DEAD_CODE_IN_TRIVIAL_IF_STATEMENT, // default is good (don't flag trivial 'if (DEBUG)')
		}));
	}

	private void checkConfigurationBlock(Class configurationBlock, HashMap coreFieldLookup) throws IllegalAccessException {
		Field[] prefFields= configurationBlock.getDeclaredFields();
		for (int i= 0; i < prefFields.length; i++) {
			Field field= prefFields[i];
			field.setAccessible(true);
			if (field.getType() == Key.class) {
				Key key= (Key)field.get(null);
				if (JavaCore.PLUGIN_ID.equals(key.getQualifier())) {
					Object fieldName= coreFieldLookup.remove(key.getName());
					assertTrue(
							"No core constant for key " + key.getName() + " in class " + configurationBlock.getName(),
							fieldName != null);
				}
			}
		}
	}
}
