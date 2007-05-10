/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.structure;

import java.util.List;

import org.eclipse.jdt.core.dom.Expression;

import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;

public interface IDefaultValueAdvisor {

	/**
	 * Create an Expression for added parameters using information from the calling scope.
	 * @param cuRewrite the CompilationUnitRewrite to use for rewrite, imports etc..
	 * @param info the added ParamterInfo object
	 * @param parameterInfos all other ParameterInfo objects
	 * @param nodes list of arguments from the calling method
	 * @param isRecursive true if called from a recursive invocation
	 * @return Expression for invocation
	 */
	Expression createDefaultExpression(CompilationUnitRewrite cuRewrite, ParameterInfo info, List parameterInfos, List nodes, boolean isRecursive);

}
