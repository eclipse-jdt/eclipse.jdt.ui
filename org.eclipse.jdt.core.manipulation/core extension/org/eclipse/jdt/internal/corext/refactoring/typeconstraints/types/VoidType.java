/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.jdt.core.Signature;


public final class VoidType extends TType {

	protected VoidType(TypeEnvironment environment) {
		super(environment, Signature.createTypeSignature("void", true)); //$NON-NLS-1$
	}

	@Override
	public int getKind() {
		return VOID_TYPE;
	}

	@Override
	public TType[] getSubTypes() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected boolean doEquals(TType type) {
		return true;
	}

	@Override
	public int hashCode() {
		return 12345;
	}

	@Override
	protected boolean doCanAssignTo(TType lhs) {
		return false;
	}

	@Override
	public String getName() {
		return "void"; //$NON-NLS-1$
	}

	@Override
	protected String getPlainPrettySignature() {
		return getName();
	}
}
