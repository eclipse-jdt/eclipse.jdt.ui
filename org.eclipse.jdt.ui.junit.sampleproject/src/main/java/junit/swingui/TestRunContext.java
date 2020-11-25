package junit.swingui;

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

import javax.swing.ListModel;

import junit.framework.Test;

/**
 * The interface for accessing the Test run context. Test run views should use
 * this interface rather than accessing the TestRunner directly.
 */
public interface TestRunContext {
	/**
	 * Handles the selection of a Test.
	 */
	void handleTestSelected(Test test);

	/**
	 * Returns the failure model
	 */
	ListModel<?> getFailures();
}
