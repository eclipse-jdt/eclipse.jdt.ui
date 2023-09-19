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
package org.eclipse.jdt.internal.corext.refactoring.structure;

import org.eclipse.core.runtime.Assert;

/**
 * Maplet from a type variable in a source class to a type variable in a target class.
 */
public final class TypeVariableMaplet {

	/** The source index */
	private final int fSourceIndex;

	/** The source of the mapping */
	private final String fSourceName;

	/** The target index */
	private final int fTargetIndex;

	/** The target of the mapping */
	private final String fTargetName;

	/**
	 * Creates a new type variable maplet.
	 *
	 * @param source
	 *        the simple name of the type variable in the source class
	 * @param index
	 *        the index of the source type variable in the source class declaration
	 * @param target
	 *        the simple name of the type variable in the range class
	 * @param offset
	 *        the index of the range type variable in the range class declaration
	 */
	public TypeVariableMaplet(final String source, final int index, final String target, final int offset) {
		Assert.isNotNull(source);
		Assert.isNotNull(target);
		Assert.isTrue(source.length() > 0);
		Assert.isTrue(target.length() > 0);
		Assert.isTrue(index >= 0);
		Assert.isTrue(offset >= 0);
		fSourceName= source;
		fTargetName= target;
		fSourceIndex= index;
		fTargetIndex= offset;
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof TypeVariableMaplet) {
			final TypeVariableMaplet mapping= (TypeVariableMaplet) object;
			return mapping.getSourceName().equals(fSourceName) && mapping.getTargetName().equals(fTargetName) && mapping.getSourceIndex() == fSourceIndex && mapping.getTargetIndex() == fTargetIndex;
		}
		return false;
	}

	/**
	 * Returns the source index of this type variable maplet.
	 *
	 * @return the source index of this maplet
	 */
	public int getSourceIndex() {
		return fSourceIndex;
	}

	/**
	 * Returns the source of this type variable maplet.
	 *
	 * @return the source of this maplet
	 */
	public String getSourceName() {
		return fSourceName;
	}

	/**
	 * Returns the target index of this type variable maplet.
	 *
	 * @return the target index of this maplet
	 */
	public int getTargetIndex() {
		return fTargetIndex;
	}

	/**
	 * Returns the target of this type variable maplet.
	 *
	 * @return the target of this maplet
	 */
	public String getTargetName() {
		return fTargetName;
	}

	@Override
	public int hashCode() {
		return fSourceIndex | fTargetIndex | fSourceName.hashCode() | fTargetName.hashCode();
	}
}
