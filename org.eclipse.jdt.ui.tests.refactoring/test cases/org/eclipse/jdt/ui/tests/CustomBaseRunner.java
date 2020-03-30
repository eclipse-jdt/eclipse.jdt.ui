/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests;

import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

public class CustomBaseRunner extends BlockJUnit4ClassRunner {
	/**
	 * @param klass
	 * @throws InitializationError
	 * @since
	 */
	public CustomBaseRunner(Class<?> klass) throws InitializationError {
        super(klass);
	    try {
	        this.filter(new InheritedTestsFilter());
	    } catch (NoTestsRemainException e) {
	        throw new IllegalStateException("class should contain at least one runnable test", e);
	    }
	}
}
