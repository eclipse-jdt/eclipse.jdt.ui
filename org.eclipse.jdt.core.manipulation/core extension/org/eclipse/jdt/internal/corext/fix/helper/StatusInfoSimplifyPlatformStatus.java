/*******************************************************************************
 * Copyright (c) 2021 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix.helper;


import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.STATUS_INFO;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;

/**
 * Change
 *
 * IStatus status = new Status(IStatus.INFO, UIPlugin.PLUGIN_ID, IStatus.OK,
 * message);
 *
 * to
 *
 * IStatus status = Status.info(message);
 *
 * since Java 9
 *
 */
public class StatusInfoSimplifyPlatformStatus extends AbstractSimplifyPlatformStatus<ClassInstanceCreation> {

	public StatusInfoSimplifyPlatformStatus() {
		super(STATUS_INFO, "IStatus.INFO"); //$NON-NLS-1$
	}

	@Override
	public String getPreview(boolean afterRefactoring) {
		if (afterRefactoring) {
			return "IStatus status = Status.info(message);\n"; //$NON-NLS-1$
		}
		return "IStatus status = new Status(IStatus.INFO, UIPlugin.PLUGIN_ID, IStatus.OK, message, null));\n"; //$NON-NLS-1$
	}
}
