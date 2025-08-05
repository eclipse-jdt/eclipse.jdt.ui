/*******************************************************************************
 * Copyright (c) 2026 Broadcom Inc. and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alex Boyko (Broadcom Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.semantictokens;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.eclipse.jdt.text.tests.AbstractSemanticHighlightingTest;

import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;

public class SemanticTokensProviderErrorTest extends AbstractSemanticHighlightingTest {

	@RegisterExtension
	public SemanticHighlightingTestSetup shts= new SemanticHighlightingTestSetup("/SHTest/src/STTest2.java");

	@Test
	public void nullTokenTypeLogsError() throws Exception {
		List<IStatus> errors= new ArrayList<>();
		ILogListener logListener= (status, plugin) -> {
			if (status.getSeverity() == IStatus.ERROR) {
				errors.add(status);
			}
		};
		Platform.addLogListener(logListener);
		try {
			setUpSemanticHighlighting(SemanticHighlightings.CLASS);

			List<IStatus> semanticErrors= errors.stream()
					.filter(s -> s.getMessage().contains("Cannot find semantic highlighting")) //$NON-NLS-1$
					.toList();
			assertFalse(semanticErrors.isEmpty(), "Expected error log for null token type but none was logged"); //$NON-NLS-1$
		} finally {
			Platform.removeLogListener(logListener);
		}
	}

}
