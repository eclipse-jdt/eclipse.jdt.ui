package org.eclipse.jdt.internal.junit.model;

public class TestRoot extends TestSuiteElement {

	public TestRoot() {
		super(null, "-1", "TESTROOT", 1); //$NON-NLS-1$//$NON-NLS-2$
	}

	public TestRoot getRoot() {
		return this;
	}
}
