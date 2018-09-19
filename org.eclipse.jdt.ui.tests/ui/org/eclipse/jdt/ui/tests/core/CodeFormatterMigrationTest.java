/*******************************************************************************
 * Copyright (c) 2018 Manumitting Technologies Inc and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.core;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.CustomProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileVersioner;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests for migrating formatter profiles.
 */
public class CodeFormatterMigrationTest extends TestCase {
	private static final Class<CodeFormatterMigrationTest> THIS= CodeFormatterMigrationTest.class;

	public CodeFormatterMigrationTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(THIS);
	}

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
}
