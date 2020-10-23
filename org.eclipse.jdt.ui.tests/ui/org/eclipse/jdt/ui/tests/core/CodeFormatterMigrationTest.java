/*******************************************************************************
 * Copyright (c) 2018, 2020 Manumitting Technologies Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Manumitting Technologies Inc - initial API and implementation
 *     Microsoft Corporation - add testUpdateFrom13_javaFormatter to test ProfileVersionerCore.updateAndComplete
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersionerCore;

/**
 * Tests for migrating formatter profiles.
 */
public class CodeFormatterMigrationTest {
	@Test
	public void test13to14_javaFormatter() {
		Map<String, String> options= new HashMap<>();
		options.put("org.eclipse.jdt.core.javaFormatter", "foo");
		int profileVersion= 13; // ProfileVersioner.VERSION_13
		String kind= null; // kind
		CustomProfile profile= new CustomProfile("13", options, profileVersion, kind);

		assertEquals(-1, ProfileVersioner.getVersionStatus(profile)); // older
		new ProfileVersioner().update(profile);

		assertEquals(0, ProfileVersioner.getVersionStatus(profile));
		assertTrue(profile.getSettings().containsKey("org.eclipse.jdt.core.javaFormatter"));
		assertEquals("foo", profile.getSettings().get("org.eclipse.jdt.core.javaFormatter"));
	}

	@Test
	public void testUpdateFrom13_javaFormatter() {
		Map<String, String> options= new HashMap<>();
		options.put("org.eclipse.jdt.core.formatter.insert_new_line_in_empty_anonymous_type_declaration", "do not insert");
		int profileVersion= 13; // ProfileVersioner.VERSION_13

		Map<String, String> newSettings = ProfileVersionerCore.updateAndComplete(options, profileVersion);

		assertTrue(newSettings.containsKey("org.eclipse.jdt.core.formatter.keep_anonymous_type_declaration_on_one_line"));
		assertEquals("one_line_if_empty", newSettings.get("org.eclipse.jdt.core.formatter.keep_anonymous_type_declaration_on_one_line"));
	}
}
