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
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

/**
 * New static methods to ease Status creation
 *
 * New API methods in Status (org.eclipse.core.runtime.Status) makes it easier
 * and less verbose to make Status object for error handling. There are methods
 * called info, warning and error for creating status objects of those
 * severities. These methods simplify the API by using StackWalker API
 * (introdcued in Java 9) to automatically determine the Plug-in ID. The
 * existing constructors for more fine grained control still continue to exist
 * and may be the most suitable when using Status objects in non-error handling
 * cases as explicitly passing the plug-in id in by String can be faster than
 * automatically determining it.
 *
 * A couple of examples of before and after with the new API:
 *
 * Creating a warning Status Existing API:
 *
 * IStatus status = new Status(IStatus.WARNING, UIPlugin.PLUGIN_ID, IStatus.OK,
 * message, null));
 *
 * New static helper methods:
 *
 * IStatus status = Status.warning(message);
 *
 * Throwing a CoreException: Existing API:
 *
 * throw new CoreException(new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID,
 * message, e));
 *
 * New static helper methods:
 *
 * throw new CoreException(Status.error(message, e));
 *
 *
 */
public class SimplifyPlatformStatusCleanUp extends AbstractCleanUp {
	private final SimplifyPlatformStatusCleanUpCore coreCleanUp= new SimplifyPlatformStatusCleanUpCore();

	public SimplifyPlatformStatusCleanUp(final Map<String, String> options) {
		setOptions(options);
	}

	public SimplifyPlatformStatusCleanUp() {
	}

	@Override
	public void setOptions(final CleanUpOptions options) {
		coreCleanUp.setOptions(options);
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(coreCleanUp.getRequirementsCore());
	}

	@Override
	public ICleanUpFix createFix(final CleanUpContext context) throws CoreException {
		ICleanUpFixCore fixCore= coreCleanUp.createFixCore(context);
		return fixCore == null ? null : new CleanUpFixWrapper(fixCore);
	}

	@Override
	public String[] getStepDescriptions() {
		return coreCleanUp.getStepDescriptions();
	}

	@Override
	public String getPreview() {
		return coreCleanUp.getPreview();
	}
}
