/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.jdt.core.refactoring.descriptors;

import java.util.Map;

import org.eclipse.jdt.core.refactoring.IJavaRefactorings;

/**
 * @since 1.24
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ConvertToRecordDescriptor extends JavaRefactoringDescriptor {

	public ConvertToRecordDescriptor(String project, String description, String comment, Map<String, String> arguments, int flags) {
		super(IJavaRefactorings.CONVERT_TO_RECORD, project, description, comment, arguments, flags);
	}

}
