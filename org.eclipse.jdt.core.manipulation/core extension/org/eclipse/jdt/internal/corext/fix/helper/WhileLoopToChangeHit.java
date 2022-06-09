/*******************************************************************************
 * Copyright (c) 2021, 2022 Carsten Hammer.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Carsten Hammer - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix.helper;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

public class WhileLoopToChangeHit {

	public boolean self;
	public VariableDeclarationStatement iteratorDeclaration;
	public Statement iteratorCall;
	public Expression collectionExpression;
	public String loopVarName;
	public MethodInvocation loopVarDeclaration;
	public WhileStatement whileStatement;
	public boolean nextWithoutVariableDeclaration;
	public boolean nextFound;
	public String iteratorName;
	public boolean isInvalid;

	public WhileLoopToChangeHit(boolean isInvalid) {
		this.isInvalid= isInvalid;
	}

	public WhileLoopToChangeHit() {

	}

}
