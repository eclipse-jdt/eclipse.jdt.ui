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

import static org.eclipse.jdt.internal.corext.fix.LibStandardNames.STATUS_ERROR;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;

/**
 * Change
 *
 * IStatus status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, IStatus.OK,
 * message, e);
 *
 * to
 *
 * IStatus status = Status.error(message,e);
 *
 * since Java 9
 *
 */
public class StatusErrorSimplifyPlatformStatus extends AbstractSimplifyPlatformStatus<ClassInstanceCreation> {

	public StatusErrorSimplifyPlatformStatus() {
		super(STATUS_ERROR, "IStatus.ERROR"); //$NON-NLS-1$
	}

	@Override
	public String getPreview(boolean afterRefactoring) {
		if (afterRefactoring) {
			return "IStatus status = Status.error(message,e);\n"; //$NON-NLS-1$
		}
		return "IStatus status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, IStatus.OK, message, e));\n"; //$NON-NLS-1$
	}
}
