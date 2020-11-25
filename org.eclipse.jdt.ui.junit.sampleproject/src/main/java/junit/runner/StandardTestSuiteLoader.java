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
 * The standard test suite loader. It can only load the same class once.
 */
public class StandardTestSuiteLoader implements TestSuiteLoader {
	/**
	 * Uses the system class loader to load the test class
	 */
	public Class load(String suiteClassName) throws ClassNotFoundException {
		return Class.forName(suiteClassName);
	}

	/**
	 * Uses the system class loader to load the test class
	 */
	public Class reload(Class aClass) throws ClassNotFoundException {
		return aClass;
	}
}
