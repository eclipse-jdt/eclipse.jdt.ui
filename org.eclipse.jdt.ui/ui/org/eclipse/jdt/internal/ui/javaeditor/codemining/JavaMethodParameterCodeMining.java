/*******************************************************************************
 * Copyright (c) 2018 Angelo Zerr and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Angelo Zerr: initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.javaeditor.codemining;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineContentCodeMining;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Expression;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class JavaMethodParameterCodeMining extends LineContentCodeMining {

	public JavaMethodParameterCodeMining(Expression parameterNode, int parameterIndex, IMethod method, boolean isVarargs, ICodeMiningProvider provider) {
		super(new Position(parameterNode.getStartPosition(), parameterNode.getLength()), provider, null);
		StringBuilder text = new StringBuilder();
		try {
			text.append(method.getParameterNames()[parameterIndex]);
			if (isVarargs && parameterIndex == method.getParameterNames().length - 1) {
				text.append('…');
			}
			text.append(": "); //$NON-NLS-1$
			setLabel(text.toString());
		} catch (JavaModelException e) {
			JavaPlugin.log(e);
		}
	}

	@Override
	public boolean isResolved() {
		return true;
	}
}
