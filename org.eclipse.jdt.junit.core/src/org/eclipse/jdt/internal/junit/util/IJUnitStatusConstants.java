/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.jdt.internal.junit.util;

public interface IJUnitStatusConstants {

	// JUnit UI status constants start at 10000 to make sure that we don't
	// collide with resource and java model constants.

	int INTERNAL_ERROR= 10001;

	/**
	 * Status constant indicating that an validateEdit call has changed the
	 * content of a file on disk.
	 */
	int VALIDATE_EDIT_CHANGED_CONTENT= 10003;

	/**
	 * Status constant indicating that junit.framework.TestCase
	 * is not on the project's path.
	 */
	int ERR_JUNIT_NOT_ON_PATH = 10004;

}
