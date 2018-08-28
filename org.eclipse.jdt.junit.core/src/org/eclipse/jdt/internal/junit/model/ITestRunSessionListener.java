/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

package org.eclipse.jdt.internal.junit.model;


public interface ITestRunSessionListener {

	/**
	 * @param testRunSession the new session, or <code>null</code>
	 */
	void sessionAdded(TestRunSession testRunSession);

	void sessionRemoved(TestRunSession testRunSession);

}
