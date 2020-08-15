package junit.runner;

/*-
 * #%L
 * org.eclipse.jdt.ui.junit.sampleproject
 * %%
 * Copyright (C) 2020 Eclipse Foundation
 * %%
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * #L%
 */

/**
 * An implementation of a TestCollector that considers a class to be a test
 * class when it contains the pattern "Test" in its name
 * 
 * @see TestCollector
 */
public class SimpleTestCollector extends ClassPathTestCollector {

	public SimpleTestCollector() {
	}

	protected boolean isTestClass(String classFileName) {
		return classFileName.endsWith(".class") && classFileName.indexOf('$') < 0 && classFileName.indexOf("Test") > 0;
	}
}
