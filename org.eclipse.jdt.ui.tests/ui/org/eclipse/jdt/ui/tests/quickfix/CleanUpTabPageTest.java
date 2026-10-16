/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.cleanup.MissingCodeTabPage;
import org.eclipse.jdt.internal.ui.preferences.formatter.IModifyDialogTabPage.IModificationListener;

public class CleanUpTabPageTest {

	private static final IModificationListener NO_OP_LISTENER= new IModificationListener() {
		@Override
		public void updateStatus(IStatus status) {
		}

		@Override
		public void valuesModified() {
		}
	};

	@Test
	public void selectionCountOfNestedOptionsStaysConsistent() {
		// the checkbox iteration order depends on identity hash codes, so use several fresh pages
		for (int i= 0; i < 50; i++) {
			Shell shell= new Shell();
			try {
				MissingCodeTabPage page= new MissingCodeTabPage();
				page.setOptionsKind(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
				page.setModifyListener(NO_OP_LISTENER);
				page.setWorkingValues(createValuesWithMissingAnnotations());
				page.createContents(shell);
				assertEquals(4, page.getSelectedCleanUpCount());

				page.doSetAll(false);
				assertEquals(0, page.getSelectedCleanUpCount());

				page.doSetAll(true);
				assertEquals(page.getCleanUpCount(), page.getSelectedCleanUpCount());

				page.doSetAll(false);
				assertEquals(0, page.getSelectedCleanUpCount());
			} finally {
				shell.dispose();
			}
		}
	}

	private static Map<String, String> createValuesWithMissingAnnotations() {
		CleanUpOptions defaults= JavaPlugin.getDefault().getCleanUpRegistry().getDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS);
		Map<String, String> values= new HashMap<>();
		for (String key : defaults.getKeys()) {
			values.put(key, CleanUpOptions.FALSE);
		}
		values.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS, CleanUpOptions.TRUE);
		values.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE, CleanUpOptions.TRUE);
		values.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_OVERRIDE_FOR_INTERFACE_METHOD_IMPLEMENTATION, CleanUpOptions.TRUE);
		values.put(CleanUpConstants.ADD_MISSING_ANNOTATIONS_DEPRECATED, CleanUpOptions.TRUE);
		return values;
	}
}
