/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jface.action.Action;

/**
 * Remove occurrence annotations action.
 *
 * @since 3.0
 */
class RemoveOccurrenceAnnotations extends Action {

	/** The Java editor to which this actions belongs. */
	private final JavaEditor fEditor;

	/**
	 * Creates this action.
	 *
	 * @param editor the Java editor for which to remove the occurrence annotations
	 */
	RemoveOccurrenceAnnotations(JavaEditor editor) {
		fEditor = editor;
	}

	/*
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		fEditor.removeOccurrenceAnnotations();
	}
}
