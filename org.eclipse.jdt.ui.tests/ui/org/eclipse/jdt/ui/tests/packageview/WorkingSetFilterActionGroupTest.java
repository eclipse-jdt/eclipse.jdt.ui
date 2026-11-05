/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.packageview;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import org.eclipse.core.runtime.IAdaptable;

import org.eclipse.jface.action.MenuManager;

import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

import org.eclipse.jdt.internal.ui.workingsets.WorkingSetFilterActionGroup;

public class WorkingSetFilterActionGroupTest {

	private IWorkingSetManager fWorkingSetManager;
	private IWorkingSet fWorkingSet;

	@BeforeEach
	public void setUp() {
		fWorkingSetManager= PlatformUI.getWorkbench().getWorkingSetManager();
		fWorkingSet= fWorkingSetManager.createWorkingSet("WorkingSetFilterActionGroupTest", new IAdaptable[0]); //$NON-NLS-1$
		fWorkingSetManager.addWorkingSet(fWorkingSet);
		fWorkingSetManager.addRecentWorkingSet(fWorkingSet);
	}

	@AfterEach
	public void tearDown() {
		fWorkingSetManager.removeWorkingSet(fWorkingSet);
	}

	@Test
	public void menuRefillReusesWorkingSetImage() {
		IWorkbenchWindow window= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		Shell shell= window.getShell();
		WorkingSetFilterActionGroup group= new WorkingSetFilterActionGroup(shell, window.getActivePage(), event -> {});
		MenuManager menuManager= new MenuManager();
		group.fillViewMenu(menuManager);
		Menu menu= menuManager.createContextMenu(shell);
		try {
			// each show rebuilds the working set items
			menu.notifyListeners(SWT.Show, new Event());
			Image image= getWorkingSetImage(menu);
			assertNotNull(image);

			menu.notifyListeners(SWT.Show, new Event());
			assertSame(image, getWorkingSetImage(menu));
			assertFalse(image.isDisposed());
		} finally {
			menu.dispose();
			menuManager.dispose();
			group.dispose();
		}
	}

	private Image getWorkingSetImage(Menu menu) {
		for (MenuItem item : menu.getItems()) {
			if (item.getText().endsWith(" " + fWorkingSet.getLabel())) //$NON-NLS-1$
				return item.getImage();
		}
		return null;
	}
}
