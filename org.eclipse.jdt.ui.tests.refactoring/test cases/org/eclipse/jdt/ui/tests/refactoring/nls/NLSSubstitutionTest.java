/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.refactoring.nls;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;

public class NLSSubstitutionTest {
	@Test
	public void generatedKey() {
		NLSSubstitution[] substitutions = {
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.0", "v1", null, null),
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.2", "v2", null, null)
				};
		setPrefix("key.", substitutions);

		NLSSubstitution subs = new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		subs.setState(NLSSubstitution.EXTERNALIZED);
	    subs.setPrefix("key.");
		subs.generateKey(substitutions, new Properties());
		assertEquals("key.1", subs.getKey());
	}

	@Test
	public void generatedKey2() {
		NLSSubstitution[] substitutions = {
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.0", "v1", null, null),
				new NLSSubstitution(NLSSubstitution.INTERNALIZED, "v2", null)
				};
		substitutions[1].setState(NLSSubstitution.EXTERNALIZED);
		setPrefix("key.", substitutions);
		substitutions[1].generateKey(substitutions, new Properties());

		NLSSubstitution subs = new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		subs.setState(NLSSubstitution.EXTERNALIZED);
	    subs.setPrefix("key.");
		subs.generateKey(substitutions, new Properties());
		assertEquals("key.2", subs.getKey());
	}

	@Test
	public void generatedKeyBug202815() {
		NLSSubstitution substitution= new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		substitution.setState(NLSSubstitution.EXTERNALIZED);
		substitution.setPrefix("key.");

		Properties properties= new Properties();
		properties.put("key.0", "v0");

		substitution.generateKey(new NLSSubstitution[] { substitution }, properties);
		assertEquals("key.1", substitution.getKey());
	}

	@Test
	public void getKeyWithoutPrefix() {
	    NLSSubstitution substitution = new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key", "value", null, null);
	    substitution.setPrefix("test.");
	    assertEquals("key", substitution.getKey());
	}

	@Test
	public void getKeyWithPrefix() {
	    NLSSubstitution substitution = new NLSSubstitution(NLSSubstitution.INTERNALIZED, "value", null);
	    substitution.setState(NLSSubstitution.EXTERNALIZED);
	    substitution.setKey("key");
	    substitution.setPrefix("test.");
	    assertEquals("test.key", substitution.getKey());
	}

	private void setPrefix(String prefix, NLSSubstitution[] subs) {
		for (NLSSubstitution sub : subs) {
			sub.setPrefix(prefix);
		}
	}

}
