/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.jdt.ui.tests.performance.views;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

public final class MockupPackageExplorerPart extends PackageExplorerPart {

	public static PackageExplorerColdPerfTest fgTest= null;

	public MockupPackageExplorerPart() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		if (fgTest != null) {
			fgTest.startMeasuring();
		}
		super.createPartControl(parent);
		if (fgTest != null)
			fgTest.finishMeasurements();
	}
}