/*******************************************************************************
 * Copyright (c) 2023 Andrey Loskutov and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.asm;

import java.util.BitSet;

import org.objectweb.asm.Opcodes;

public class DecompilerOptions {

	public static final int LATEST_ASM_VERSION = Opcodes.ASM9;

	public final String fieldFilter;

	public final String methodFilter;

	public final BitSet modes;

	public DecompilerOptions(final String fieldFilter, final String methodFilter, final BitSet modes) {
		this.fieldFilter = fieldFilter;
		this.methodFilter = methodFilter;
		this.modes = modes;
	}
}
