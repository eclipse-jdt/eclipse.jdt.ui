/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Red Hat Inc. - modified to use VariableDeclarationCleanUpCore
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.fix;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.manipulation.ICleanUpFixCore;

import org.eclipse.jdt.ui.cleanup.CleanUpContext;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpRequirements;
import org.eclipse.jdt.ui.cleanup.ICleanUpFix;

public class VariableDeclarationCleanUp extends AbstractCleanUp {

	private VariableDeclarationCleanUpCore coreCleanUp= new VariableDeclarationCleanUpCore();

	public VariableDeclarationCleanUp(Map<String, String> options) {
		super();
		setOptions(options);
	}

	public VariableDeclarationCleanUp() {
		super();
	}

	@Override
	public CleanUpRequirements getRequirements() {
		return new CleanUpRequirements(coreCleanUp.getRequirementsCore());
	}

	@Override
	public void setOptions(CleanUpOptions options) {
		coreCleanUp.setOptions(options);
	}

	@Override
	public ICleanUpFix createFix(CleanUpContext context) throws CoreException {
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
