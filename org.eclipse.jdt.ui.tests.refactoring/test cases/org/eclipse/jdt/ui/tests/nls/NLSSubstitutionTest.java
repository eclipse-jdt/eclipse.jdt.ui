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
		NLSSubstitution[] substitutions = {
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.0", "v1", null, null),
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.2", "v2", null, null)
				};
		
		NLSSubstitution subs = new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		subs.setState(NLSSubstitution.EXTERNALIZED);
		subs.generateKey(substitutions, "key.");
		assertEquals(subs.getKey(), "1");
	}
	
	public void testGeneratedKey2() {
		NLSSubstitution[] substitutions = {
				new NLSSubstitution(NLSSubstitution.EXTERNALIZED, "key.0", "v1", null, null),
				new NLSSubstitution(NLSSubstitution.INTERNALIZED, "v2", null)
				};
		substitutions[1].setState(NLSSubstitution.EXTERNALIZED);
		substitutions[1].generateKey(substitutions, "key.");
			
		NLSSubstitution subs = new NLSSubstitution(NLSSubstitution.IGNORED, "v1", null);
		subs.setState(NLSSubstitution.EXTERNALIZED);
		subs.generateKey(substitutions, "key.");
		assertEquals(subs.getKey(), "2");
	}
}
