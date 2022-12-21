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
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.eclipse.core.runtime.Assert;


public final class ConstraintOperator {

	private final String fOperatorString;
	private final int fOperatorCode;

	private static final int CODE_SUBTYPE= 0;
	private static final int CODE_EQUALS= 1;
	private static final int CODE_DEFINES= 2;
	private static final int CODE_STRICT_SUBTYPE= 3;

	private static final String STRING_SUBTYPE= "<=";//$NON-NLS-1$
	private static final String STRING_EQUALS= "==";//$NON-NLS-1$
	private static final String STRING_DEFINES= "=^=";//$NON-NLS-1$
	private static final String STRING_STRICT_SUBTYPE= "<";//$NON-NLS-1$
	private static final Collection<String> fgOperatorStrings= new HashSet<>(Arrays.asList(STRING_SUBTYPE, STRING_EQUALS, STRING_DEFINES, STRING_STRICT_SUBTYPE));

	private static final ConstraintOperator fgSubtype= new ConstraintOperator(STRING_SUBTYPE, CODE_SUBTYPE);
	private static final ConstraintOperator fgEquals= new ConstraintOperator(STRING_EQUALS, CODE_EQUALS);
	private static final ConstraintOperator fgDefines= new ConstraintOperator(STRING_DEFINES, CODE_DEFINES);
	private static final ConstraintOperator fgStrictSubtype= new ConstraintOperator(STRING_STRICT_SUBTYPE, CODE_STRICT_SUBTYPE);

	public static ConstraintOperator createSubTypeOperator() {
		return fgSubtype;
	}

	public static ConstraintOperator createEqualsOperator() {
		return fgEquals;
	}

	public static ConstraintOperator createDefinesOperator() {
		return fgDefines;
	}

	public static ConstraintOperator createStrictSubtypeOperator() {
		return fgStrictSubtype;
	}

	private ConstraintOperator(String string, int code){
		Assert.isTrue(fgOperatorStrings.contains(string));
		Assert.isTrue(code == CODE_DEFINES || code == CODE_EQUALS || code == CODE_STRICT_SUBTYPE || code == CODE_SUBTYPE);
		fOperatorString= string;
		fOperatorCode= code;
	}

	public String getOperatorString(){
		return fOperatorString;
	}

	@Override
	public String toString() {
		return getOperatorString();
	}

	@Override
	public boolean equals(Object obj) {
		if (! (obj instanceof ConstraintOperator))
			return false;
		ConstraintOperator other= (ConstraintOperator)obj;
		return fOperatorCode == other.fOperatorCode;
	}

	@Override
	public int hashCode() {
		return fOperatorString.hashCode();
	}

	public boolean isSubtypeOperator() {
		return fOperatorCode == CODE_SUBTYPE;
	}

	public boolean isStrictSubtypeOperator() {
		return fOperatorCode == CODE_STRICT_SUBTYPE;
	}

	public boolean isEqualsOperator() {
		return fOperatorCode == CODE_EQUALS;
	}

	public boolean isDefinesOperator() {
		return fOperatorCode == CODE_DEFINES;
	}
}
