/*******************************************************************************
 * Copyright (c) 2016, 2017 IBM Corporation and others.
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

package org.eclipse.jdt.internal.junit5.runner;

import java.text.MessageFormat;
import java.util.function.Function;

import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;

import org.eclipse.jdt.internal.junit.runner.ITestIdentifier;
import org.eclipse.jdt.internal.junit.runner.MessageIds;

public class JUnit5Identifier implements ITestIdentifier {

	private TestIdentifier fTestIdentifier;

	public JUnit5Identifier(TestIdentifier testIdentifier) {
		fTestIdentifier= testIdentifier;
	}

	@Override
	public String getName() {
		return fTestIdentifier.getSource().map(this::getName).orElse(fTestIdentifier.getDisplayName());
	}

	private String getName(TestSource testSource) {
		if (testSource instanceof ClassSource) {
			return ((ClassSource) testSource).getJavaClass().getName();
		}
		if (testSource instanceof MethodSource) {
			MethodSource methodSource= (MethodSource) testSource;
			return MessageFormat.format(MessageIds.TEST_IDENTIFIER_MESSAGE_FORMAT, methodSource.getMethodName(), methodSource.getClassName());
		}
		return null;
	}

	@Override
	public String getDisplayName() {
		return fTestIdentifier.getDisplayName();
	}

	@Override
	public int hashCode() {
		return fTestIdentifier.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JUnit5Identifier))
			return false;

		JUnit5Identifier id= (JUnit5Identifier) obj;
		return fTestIdentifier.equals(id.fTestIdentifier);
	}

	@Override
	public String getParameterTypes() {
		Function<TestSource, String> getParameterTypes= (TestSource source) -> (source instanceof MethodSource ? ((MethodSource) source).getMethodParameterTypes() : null);
		return fTestIdentifier.getSource().map(getParameterTypes).orElse(""); //$NON-NLS-1$
	}

	@Override
	public String getUniqueId() {
		return fTestIdentifier.getUniqueId();
	}
}
