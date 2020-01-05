/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 *     Red Hat Inc. - copied from JdtViewerDropAdapter
 *******************************************************************************/
package org.eclipse.jdt.internal.ui;

public class JdtViewerDropAdapterConstants {

	/**
	 * Constant describing the position of the cursor relative
	 * to the target object.  This means the mouse is positioned
	 * slightly before the target.
	 */
	public static final int LOCATION_BEFORE = 1;

	/**
	 * Constant describing the position of the cursor relative
	 * to the target object.  This means the mouse is positioned
	 * slightly after the target.
	 */
	public static final int LOCATION_AFTER = 2;

	/**
	 * Constant describing the position of the cursor relative
	 * to the target object.  This means the mouse is positioned
	 * directly on the target.
	 */
	public static final int LOCATION_ON = 3;

	/**
	 * Constant describing the position of the cursor relative
	 * to the target object.  This means the mouse is not positioned
	 * over or near any valid target.
	 */
	public static final int LOCATION_NONE = 4;

	private JdtViewerDropAdapterConstants() {
	}

}
