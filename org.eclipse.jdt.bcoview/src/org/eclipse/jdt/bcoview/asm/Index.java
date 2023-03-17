/*******************************************************************************
 * Copyright (c) 2023 Eric Bruneton and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eric Bruneton - initial API and implementation
 *     Andrey Loskutov - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.bcoview.asm;

import org.objectweb.asm.tree.LabelNode;

public class Index {

	public final LabelNode labelNode;

	public final int insn;

	public final int opcode;

	public Index(final LabelNode label, final int insn, final int opcode) {
		this.labelNode = label;
		this.insn = insn;
		this.opcode = opcode;
	}
}
