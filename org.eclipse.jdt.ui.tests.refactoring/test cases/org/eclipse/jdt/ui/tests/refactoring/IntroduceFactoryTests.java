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
 *     Samrat Dhillon <samrat.dhillon@gmail.com> - [introduce factory] Introduce Factory on an abstract class adds a statement to create an instance of that class - https://bugs.eclipse.org/bugs/show_bug.cgi?id=395016
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import org.junit.Test;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import org.eclipse.jdt.ui.tests.refactoring.rules.Java1d6Setup;

/**
 * @author rfuhrer@watson.ibm.com
 */
public class IntroduceFactoryTests extends IntroduceFactoryTestsBase{
	private static final String REFACTORING_PATH= "IntroduceFactory/";

	public IntroduceFactoryTests() {
		rts= new Java1d6Setup();
	}

	@Override
	protected String getRefactoringPath() {
		return REFACTORING_PATH;
	}

	//--- TESTS
	@Test
	public void testStaticContext_FFF() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	@Test
	public void testInstanceContext_FFF() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	static final String[]	k_Names = { "createThing", "ThingFactory", "IThingFactory" };

	@Test
	public void testNames_FFF() throws Exception {
		namesHelper(k_Names[0], null);
	}
	//
	// ================================================================================
	//
	@Test
	public void testMultipleCallers_FFF() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	@Test
	public void testSelectConstructor() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	@Test
	public void testDifferentSigs() throws Exception {
		singleUnitHelper(false);
	}

	@Test
	public void testDifferentArgs1() throws Exception {
		singleUnitHelper(false);
	}

	@Test
	public void testDifferentArgs2() throws Exception {
		singleUnitHelper(false);
	}

	@Test
	public void testDifferentArgs3() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	@Test
	public void testUnmovableArg1() throws Exception {
		singleUnitHelper(false);
	}

	@Test
	public void testUnmovableArg2() throws Exception {
		singleUnitHelper(false);
	}

	@Test
	public void testDontMoveArgs1() throws Exception {
		singleUnitHelper(false);
	}

	@Test
	public void testDontMoveArgs2() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	@Test
	public void testProtectConstructor1() throws Exception {
		singleUnitHelper(true);
	}

	@Test
	public void testProtectConstructor2() throws Exception {
		singleUnitHelper(true);
	}
	//
	// ================================================================================
	//
	@Test
	public void testStaticInstance() throws Exception {
		singleUnitHelper(false);
	}
	//
	// ================================================================================
	//
	@Test
	public void testCtorThrows() throws Exception {
		singleUnitHelper(true);
	}
	//
	// ================================================================================
	//
	@Test
	public void testJavadocRef() throws Exception {
		singleUnitHelper(true);
	}
	//
	// ================================================================================
	//
	@Test
	public void testNestedClass() throws Exception {
		failHelper(RefactoringStatus.FATAL);
	}



    //
    // ================================================================================
    // Generics-related tests
	@Test
	public void testTypeParam() throws Exception {
        singleUnitHelper(true);
    }

	@Test
	public void testTwoTypeParams() throws Exception {
        singleUnitHelper(true);
    }

	@Test
	public void testBoundedTypeParam() throws Exception {
        singleUnitHelper(true);
    }

	@Test
	public void testTwoBoundedTypeParams() throws Exception {
        singleUnitHelper(true);
    }

	@Test
	public void testWildcardParam() throws Exception {
		singleUnitHelper(true);
	}

	@Test
	public void testTypeParam2() throws Exception {
        namesHelper(null, "p.Factory");
    }
    //
	// ================================================================================
	// Other J2SE 5.0 tests
	@Test
	public void testEnum() throws Exception {
    	failHelper(RefactoringStatus.FATAL);
    }

	@Test
	public void testAnnotation1() throws Exception {
   		singleUnitHelper(true);
    }

	@Test
	public void testAnnotation2() throws Exception {
   		singleUnitHelper(true);
    }

	@Test
	public void testAnnotation3() throws Exception {
   		singleUnitHelper(true);
    }

	@Test
	public void testVarArgsCtor() throws Exception {
	    // RMF - As of I20050202, search engine doesn't reliably find call sites to varargs methods
		singleUnitHelper(true);
	}
    //
	// ================================================================================
	//
	@Test
	public void testMultipleUnits_FFF() throws Exception {
		multiUnitHelper(false, new String[] { "MultiUnit1A", "MultiUnit1B", "MultiUnit1C" });
	}
	//
	// ================================================================================
	// Bugzilla bug regression tests
	// ================================================================================
	//
	@Test
	public void test45942() throws Exception {
		multiUnitBugHelper(true, new String[] { "TestClass", "UseTestClass" }, null);
	}

	@Test
	public void test46189() throws Exception {
		singleUnitBugHelper("TestClass", true);
	}

	@Test
	public void test46189B() throws Exception {
		singleUnitBugHelper("TestClass", true);
	}

	@Test
	public void test46373() throws Exception {
		singleUnitBugHelper("ImplicitCtor", false);
	}

	@Test
	public void test46374() throws Exception {
		singleUnitBugHelper("QualifiedName", false);
	}

	@Test
	public void test46608() throws Exception {
		multiUnitBugHelper(true, new String[] { "p1/TT", "p2/TT" }, null);
	}

	@Test
	public void test59284() throws Exception {
		singleUnitBugHelper("ArgTypeImport", true);
	}

	@Test
	public void test59280() throws Exception {
		singleUnitBugHelper("ExplicitSuperCtorCall", true);
	}

	@Test
	public void test48504() throws Exception {
		multiUnitBugHelper(true, new String[] { "p1/A", "p1/B" }, "p1.B");
	}

	@Test
	public void test58293() throws Exception {
		singleUnitBugHelper("ImplicitSuperCtorCall", true);
	}

	@Test
	public void test59283() throws Exception {
		multiProjectBugHelper(new String[] { "proj1/pA/A", "proj2/pB/B" },
				new String[] { "proj2:proj1" });
	}

	@Test
	public void test84807() throws Exception {
		singleUnitBugHelper("CtorOfParamType", true);
	}

	@Test
	public void test85465() throws Exception {
		singleUnitBugHelper("Varargs1", true);
	}

	@Test
	public void test97507() throws Exception {
		singleUnitBugHelper("CtorTypeArgBounds", true);
	}

	@Test
	public void test250660() throws Exception {
		singleUnitBugHelper("HasAnonymous", true);
	}

	@Test
	public void test74759() throws Exception {
		singleUnitBugHelper("Test", true);
	}

	@Test
	public void test298281() throws Exception {
		singleUnitBugHelper("Thing", true);
	}

	@Test
	public void test395016_1() throws Exception {
		singleUnitBugHelperWithWarning("AbstractClass", true);
	}

	@Test
	public void test395016_2() throws Exception {
		singleUnitBugHelperWithWarning("AbstractMethod", true);
	}

	@Test
	public void testFactoryClash() throws Exception {
		failHelper(RefactoringStatus.ERROR);
	}
}
