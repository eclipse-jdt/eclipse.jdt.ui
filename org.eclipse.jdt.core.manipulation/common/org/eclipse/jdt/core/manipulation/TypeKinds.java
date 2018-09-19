/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package org.eclipse.jdt.core.manipulation;

/**
 * Contains various properties for a Java element.
 *
 * @since 1.10
 */
public class TypeKinds {

	public static final int CLASSES= 1 << 1;
	public static final int INTERFACES= 1 << 2;
	public static final int ANNOTATIONS= 1 << 3;
	public static final int ENUMS= 1 << 4;
	public static final int VARIABLES= 1 << 5;
	public static final int PRIMITIVETYPES= 1 << 6;
	public static final int VOIDTYPE= 1 << 7;
	public static final int REF_TYPES= CLASSES | INTERFACES | ENUMS | ANNOTATIONS;
	public static final int REF_TYPES_AND_VAR= REF_TYPES | VARIABLES;
	public static final int ALL_TYPES= PRIMITIVETYPES | REF_TYPES_AND_VAR;

	private TypeKinds () {
	}

}
