/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring.nls;

import java.util.Properties;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSSubstitution;

public class NLSSubstitutionTest extends TestCase {

	public NLSSubstitutionTest(String name) {
		super(name);
	}

	public static TestSuite suite() {
		return new TestSuite(NLSSubstitutionTest.class);
	}

	public void testGeneratedKey() {
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

	public void testGeneratedKey2() {
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

	public void testGeneratedKeyBug202815() {
		NLSSubstitution substitution= new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		substitution.setState(NLSSubstitution.EXTERNALIZED);
		substitution.setPrefix("key.");

		Properties properties= new Properties();
		properties.put("key.0", "v0");

		substitution.generateKey(new NLSSubstitution[] { substitution }, properties);
		assertEquals("key.1", substitution.getKey());
	}

	public void testGetKeyWithoutPrefix() {
	    NLSSubstitution substitution = new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key", "value", null, null);
	    substitution.setPrefix("test.");
	    assertEquals("key", substitution.getKey());
	}

	public void testGetKeyWithPrefix() {
	    NLSSubstitution substitution = new NLSSubstitution(NLSSubstitution.INTERNALIZED, "value", null);
	    substitution.setState(NLSSubstitution.EXTERNALIZED);
	    substitution.setKey("key");
	    substitution.setPrefix("test.");
	    assertEquals("test.key", substitution.getKey());
	}

	private void setPrefix(String prefix, NLSSubstitution[] subs) {
		for (int i= 0; i < subs.length; i++)
			subs[i].setPrefix(prefix);
	}

}
