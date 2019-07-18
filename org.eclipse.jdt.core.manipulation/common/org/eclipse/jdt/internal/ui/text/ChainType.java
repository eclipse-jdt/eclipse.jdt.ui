/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat - Initial Contribution
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text;

import org.eclipse.jdt.core.IType;

/**
 * Unify IType and primitive types so they may be treated the same way
 * in the chain completion logic.
 */
public class ChainType {

	private IType type;

	private String primitiveType;

	private int dimension;

	public ChainType (IType type) {
		this.type= type;
	}

	public ChainType (String primitiveType, int dimension) {
		this.primitiveType= primitiveType;
		this.dimension= dimension;
	}

	public ChainType (String primitiveType) {
		this.primitiveType= primitiveType;
	}

	public IType getType() {
		return type;
	}

	public String getPrimitiveType() {
		return primitiveType;
	}

	public int getDimension () {
		return dimension;
	}

	@Override
	public String toString() {
		return (type != null) ? type.getFullyQualifiedName() : primitiveType;
	}
}
