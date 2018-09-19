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

public final class UnboundWildcardType extends WildcardType {

	protected UnboundWildcardType(TypeEnvironment environment) {
		super(environment);
	}

	@Override
	public int getKind() {
		return UNBOUND_WILDCARD_TYPE;
	}

	@Override
	public TType getErasure() {
		return getEnvironment().getJavaLangObject();
	}

	@Override
	protected boolean doCanAssignTo(TType lhs) {
		switch(lhs.getKind()) {
			case STANDARD_TYPE:
				return ((StandardType)lhs).isJavaLangObject();
			case UNBOUND_WILDCARD_TYPE:
				return true;
			case SUPER_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE:
				return ((WildcardType)lhs).getBound().isJavaLangObject();
			case CAPTURE_TYPE:
				return ((CaptureType)lhs).checkLowerBound(this);
			default:
				return false;
		}
	}

	@Override
	protected boolean checkTypeArgument(TType rhs) {
		switch(rhs.getKind()) {
			case ARRAY_TYPE:
			case STANDARD_TYPE:
			case PARAMETERIZED_TYPE:
			case RAW_TYPE:
			case UNBOUND_WILDCARD_TYPE:
			case EXTENDS_WILDCARD_TYPE:
			case SUPER_WILDCARD_TYPE:
			case TYPE_VARIABLE:
			case CAPTURE_TYPE:
				return true;
			default:
				return false;
		}
	}

	@Override
	protected boolean checkAssignmentBound(TType rhs) {
		// unbound equals ? extends Object.
		return rhs.isNullType();
	}

	@Override
	public String getName() {
		return "?"; //$NON-NLS-1$
	}

	@Override
	protected String getPlainPrettySignature() {
		return getName();
	}
}
