/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.dom;

import org.eclipse.jdt.core.dom.AST;

public interface IASTSharedValues {

	/**
	 * This value is subject to change with every release. JDT-UI-internal code typically supports
	 * the latest available {@link AST#apiLevel() AST level} exclusively.
	 */
	int SHARED_AST_LEVEL= AST.getJLSLatest();

	boolean SHARED_AST_STATEMENT_RECOVERY= true;

	boolean SHARED_BINDING_RECOVERY= true;
}
