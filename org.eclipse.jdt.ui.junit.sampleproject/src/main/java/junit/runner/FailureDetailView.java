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

import java.awt.Component;

import junit.framework.*;

/**
 * A view to show a details about a failure
 */
public interface FailureDetailView {
	/**
	 * Returns the component used to present the TraceView
	 */
	Component getComponent();

	/**
	 * Shows details of a TestFailure
	 */
	void showFailure(TestFailure failure);

	/**
	 * Clears the view
	 */
	void clear();
}
