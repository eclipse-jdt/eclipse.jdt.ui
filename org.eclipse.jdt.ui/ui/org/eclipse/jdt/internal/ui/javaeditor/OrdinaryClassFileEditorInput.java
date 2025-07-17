/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor;

import org.eclipse.jdt.core.IOrdinaryClassFile;

public class OrdinaryClassFileEditorInput extends InternalClassFileEditorInput {

	public OrdinaryClassFileEditorInput(IOrdinaryClassFile classFile) {
		super(classFile);
	}

	@Override
	public IOrdinaryClassFile getClassFile() {
		return (IOrdinaryClassFile) super.getClassFile();
	}

}
