/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.jdt.internal.corext.codemanipulation.NameProposer;


public class NameProposerTest extends TestCase {
	
	private static final Class THIS= NameProposerTest.class;

	public NameProposerTest(String name) {
		super(name);
	}

	public static Test suite() {
		if (true) {
			return new TestSuite(THIS);
		} else {
			TestSuite suite= new TestSuite();
			suite.addTest(new NameProposerTest("testGetterSetterName"));
			return suite;
		}	
	}

	protected void setUp() throws Exception {
	}


	protected void tearDown() throws Exception {
	}


	public void testGetterSetterName() throws Exception {
		
		String[] prefixes= new String[] { "f", "fg" };
		String[] suffixes= new String[] { "_" };
		
		NameProposer proposer= new NameProposer(prefixes, suffixes);
		
		assertEquals("setCount", proposer.proposeSetterName("fCount", false));
		assertEquals("getCount", proposer.proposeGetterName("fCount", false));
		assertEquals("setSingleton", proposer.proposeSetterName("fgSingleton", false));
		assertEquals("getSingleton", proposer.proposeGetterName("fgSingleton", false));
		assertEquals("setFoo", proposer.proposeSetterName("foo", false));
		assertEquals("getFoo", proposer.proposeGetterName("foo", false));
		
		assertEquals("setBlue", proposer.proposeSetterName("fBlue", true));
		assertEquals("isBlue", proposer.proposeGetterName("fBlue", true));
		assertEquals("setModified", proposer.proposeSetterName("modified", true));
		assertEquals("isModified", proposer.proposeGetterName("modified", true));
		assertEquals("setTouched", proposer.proposeSetterName("isTouched", true));
		assertEquals("isTouched", proposer.proposeGetterName("isTouched", true));		
	}
	
}
