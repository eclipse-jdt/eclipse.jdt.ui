package org.eclipse.jdt.ui.tests.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
  */
public class CoreTests extends TestCase {

	public static Test suite() {
		
		TestSuite suite= new TestSuite();
		suite.addTest(new TestSuite(AddImportTest.class));
		suite.addTest(new TestSuite(AddUnimplementedMethodsTest.class));
		//suite.addTest(new TestSuite(AllTypesCacheTest.suite.class));
		suite.addTest(new TestSuite(BindingsNameTest.class));
		suite.addTest(new TestSuite(ClassPathDetectorTest.class));
		suite.addTest(new TestSuite(HierarchicalASTVisitorTest.class));
		suite.addTest(new TestSuite(ImportOrganizeTest.class));
		suite.addTest(new TestSuite(JavaModelUtilTest.class));
		//suite.addTest(new TestSuite(NameProposerTest.class));
		suite.addTest(new TestSuite(TextBufferTest.class));
		suite.addTest(new TestSuite(TypeInfoTest.class));		
		return suite;
	}

	public CoreTests(String name) {
		super(name);
	}
	
	
}
