
package org.eclipse.jdt.internal.junit.runner;

import junit.framework.Test;

public class NullPrioritizer implements ITestPrioritizer {

	public Test prioritize(Test input) {
		return input;
	}
}
