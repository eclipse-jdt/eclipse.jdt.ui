/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.performance.views;

import org.eclipse.swt.widgets.Composite;

import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;

import org.eclipse.test.performance.Dimension;

public final class MockupPackageExplorerPart extends PackageExplorerPart {

	public static PackageExplorerColdPerfTest fgTest= null;

	public MockupPackageExplorerPart() {
		super();
	}

	public final void createPartControl(Composite parent) {
		if (fgTest != null) {
			fgTest.tagAsGlobalSummary("Open Package Explorer - PackageExplorerPart#createPartControl", Dimension.CPU_TIME);
			fgTest.startMeasuring();
		}
		super.createPartControl(parent);
		if (fgTest != null)
			fgTest.finishMeasurements();
	}
}