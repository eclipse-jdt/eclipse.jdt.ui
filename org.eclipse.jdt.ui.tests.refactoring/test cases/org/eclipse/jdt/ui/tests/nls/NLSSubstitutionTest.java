package org.eclipse.jdt.ui.tests.nls;

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
	    NLSSubstitution.setPrefix("key.");
		NLSSubstitution[] substitutions = {
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.0", "v1", null, null),
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.2", "v2", null, null)
				};
		
		NLSSubstitution subs = new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		subs.setState(NLSSubstitution.EXTERNALIZED);
		subs.generateKey(substitutions);
		assertEquals(subs.getKey(), "key.1");
	}
	
	public void testGeneratedKey2() {
	    NLSSubstitution.setPrefix("key.");
		NLSSubstitution[] substitutions = {
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.0", "v1", null, null),
				new NLSSubstitution(NLSSubstitution.INTERNALIZED, "v2", null)
				};
		substitutions[1].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[1].generateKey(substitutions);
			
		NLSSubstitution subs = new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		subs.setState(NLSSubstitution.EXTERNALIZED);
		subs.generateKey(substitutions);
		assertEquals(subs.getKey(), "key.2");
	}
	
	public void testGetKeyWithoutPrefix() {
	    NLSSubstitution.setPrefix("test.");
	    NLSSubstitution substitution = new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key", "value", null, null);
	    assertEquals("key", substitution.getKey());
	}
	
	public void testGetKeyWithPrefix() {
	    NLSSubstitution.setPrefix("test.");
	    NLSSubstitution substitution = new NLSSubstitution(NLSSubstitution.INTERNALIZED, "value", null);
	    substitution.setState(NLSSubstitution.EXTERNALIZED);
	    substitution.setKey("key");
	    assertEquals("test.key", substitution.getKey());
	}
}
