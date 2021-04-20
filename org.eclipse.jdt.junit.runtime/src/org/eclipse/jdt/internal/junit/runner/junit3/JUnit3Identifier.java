/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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
 *     David Saff (saff@mit.edu) - initial API and implementation
 *             (bug 102632: [JUnit] Support for JUnit 4.)
 *******************************************************************************/

/**
 *
 */
package org.eclipse.jdt.internal.junit.runner.junit3;

import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;

class JUnit3Identifier implements ITestIdentifier {
	private final JUnit3TestReference ref;

	JUnit3Identifier(JUnit3TestReference ref) {
		this.ref = ref;
	}

	@Override
	public String getName() {
		return ref.getName();
	}

	@Override
	public boolean equals(Object obj) {
		JUnit3Identifier id = (JUnit3Identifier) obj;
		return ref.equals(id.ref);
	}

	@Override
	public int hashCode() {
		return ref.hashCode();
	}

	@Override
	public String getDisplayName() {
		return getName();
	}

	@Override
	public String getParameterTypes() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getUniqueId() {
		return ""; //$NON-NLS-1$
	}
}