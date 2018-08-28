/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.core.refactoring.participants;

import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;

/**
 * A participant to participate in refactorings that change method signatures.
 * <p>
 * Change method signature participants are registered via the extension point <code>
 * org.eclipse.jdt.core.manipulation.changeMethodSignatureParticipants</code>.
 * Extensions to this extension point must extend this abstract class.
 * </p>
 *
 * @since 1.2
 */
public abstract class ChangeMethodSignatureParticipant extends RefactoringParticipant {

	private ChangeMethodSignatureArguments fArguments;

	@Override
	protected final void initialize(RefactoringArguments arguments) {
		fArguments= (ChangeMethodSignatureArguments) arguments;
	}

	/**
	 * Returns the change method signature arguments.
	 *
	 * @return the change method signature arguments
	 */
	public ChangeMethodSignatureArguments getArguments() {
		return fArguments;
	}
}
