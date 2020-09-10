/*******************************************************************************
 * Copyright (c) 2008, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;

import org.eclipse.jdt.core.JavaCore;

import org.eclipse.jdt.internal.ui.preferences.ComplianceConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.JavaBuildConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.JavadocProblemsConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.NameConventionConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.OptionsConfigurationBlock.Key;
import org.eclipse.jdt.internal.ui.preferences.ProblemSeveritiesConfigurationBlock;
import org.eclipse.jdt.internal.ui.preferences.TodoTaskConfigurationBlock;

public class OptionsConfigurationBlockTest {

	/**
	 * Reflective test that ensures that all options from {@link JavaCore} are used in the UI.
	 *
	 * @throws Exception should not
	 */
	@Test
	public void testKeysForOptions() throws Exception {
		HashMap<String, String> coreFieldLookup= new HashMap<>();
		for (Field field : JavaCore.class.getDeclaredFields()) {
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
		Class<?> codeAssistAdvancedConfigurationBlock= Class.forName("org.eclipse.jdt.internal.ui.preferences.CodeAssistAdvancedConfigurationBlock");
		checkConfigurationBlock(codeAssistAdvancedConfigurationBlock, coreFieldLookup);
		Class<?> codeAssistConfigurationBlock= Class.forName("org.eclipse.jdt.internal.ui.preferences.CodeAssistConfigurationBlock");
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
	@Deprecated
	private void removeUnusedOptions(HashMap<String, String> coreFieldLookup) {
		coreFieldLookup.keySet().removeAll(Arrays.asList(
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

				JavaCore.CODEASSIST_DISCOURAGED_REFERENCE_CHECK, // is on the Type Filters page now, see https://bugs.eclipse.org/218487
				JavaCore.CODEASSIST_FORBIDDEN_REFERENCE_CHECK));   // is on the Type Filters page now, see https://bugs.eclipse.org/218487
	}

	private void checkConfigurationBlock(Class<?> configurationBlock, HashMap<String, String> coreFieldLookup) throws Exception {
		Method keysMethod;
		try {
			keysMethod= configurationBlock.getDeclaredMethod("getKeys");
		} catch (NoSuchMethodException e) {
			try {
				keysMethod= configurationBlock.getDeclaredMethod("getAllKeys");
			} catch (NoSuchMethodException e1) {
				keysMethod= configurationBlock.getDeclaredMethod("getKeys", boolean.class);
			}
		}
		keysMethod.setAccessible(true);
		Key[] keys= (Key[]) (keysMethod.getParameterTypes().length > 0 ? keysMethod.invoke(null, Boolean.FALSE) : keysMethod.invoke(null));
		HashSet<Key> keySet= new HashSet<>(Arrays.asList(keys));

		for (Field field : configurationBlock.getDeclaredFields()) {
			field.setAccessible(true);
			if (field.getType() == Key.class) {
				Key key= (Key)field.get(null);
				boolean keyWasInKeySet= keySet.remove(key);
				if (JavaCore.PLUGIN_ID.equals(key.getQualifier())) {
					Object fieldName= coreFieldLookup.remove(key.getName());
					assertNotNull("No core constant for key " + key.getName() + " in class " + configurationBlock.getName(), fieldName);
					assertTrue(configurationBlock.getName() + "#getKeys() is missing key '" + key.getName() + "'", keyWasInKeySet);
				}
			}
		}

		assertEquals(configurationBlock.getName() + "#getKeys() includes keys that are not declared in the class", Collections.emptySet(), keySet);
	}
}
