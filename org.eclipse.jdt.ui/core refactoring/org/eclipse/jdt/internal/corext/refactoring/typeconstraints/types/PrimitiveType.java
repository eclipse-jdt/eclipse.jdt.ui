/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.typeconstraints.types;

import org.eclipse.jdt.core.IJavaProject;


public class PrimitiveType extends Type {

	/** Type code for the primitive type "int". */
	public static final int INT = 0;
	/** Type code for the primitive type "char". */
	public static final int CHAR = 1;
	/** Type code for the primitive type "boolean". */
	public static final int BOOLEAN = 2;
	/** Type code for the primitive type "short". */
	public static final int SHORT = 3;
	/** Type code for the primitive type "long". */
	public static final int LONG = 4;
	/** Type code for the primitive type "float". */
	public static final int FLOAT = 5;
	/** Type code for the primitive type "double". */
	public static final int DOUBLE = 6;
	/** Type code for the primitive type "byte". */
	public static final int BYTE = 7;
	
	static final String[] NAMES= {
		"int",  //$NON-NLS-1$
		"char",  //$NON-NLS-1$
		"boolean",  //$NON-NLS-1$
		"short",  //$NON-NLS-1$
		"long",  //$NON-NLS-1$
		"float",  //$NON-NLS-1$
		"double",  //$NON-NLS-1$
		"byte"};  //$NON-NLS-1$
	
	private int fId;
	
	protected PrimitiveType(TypeEnvironment environment, int id) {
		super(environment);
		fId= id;
	}
	
	public int getId() {
		return fId;
	}
	
	public int getElementType() {
		return PRIMITIVE_TYPE;
	}
	
	protected boolean doEquals(Type type) {
		return fId == ((PrimitiveType)type).fId;
	}

	protected boolean doCanAssignTo(Type target) {
		if (target.getElementType() != PRIMITIVE_TYPE) {
			if (target.getElementType() == STANDARD_TYPE) {
				IJavaProject javaProject= ((StandardType)target).getJavaElementType().getJavaProject();
				return getEnvironment().createBoxed(this, javaProject).canAssignTo(target);
			}
			return false;
		}
		
		switch (((PrimitiveType)target).fId) {
			case BOOLEAN :
			case BYTE :
			case CHAR :
				return false;
			case DOUBLE :
				switch (fId) {
					case BYTE :
					case CHAR :
					case SHORT :
					case INT :
					case LONG :
					case FLOAT :
						return true;
					default :
						return false;
				}
			case FLOAT :
				switch (fId) {
					case BYTE :
					case CHAR :
					case SHORT :
					case INT :
					case LONG :
						return true;
					default :
						return false;
				}
			case LONG :
				switch (fId) {
					case BYTE :
					case CHAR :
					case SHORT :
					case INT :
						return true;
					default :
						return false;
				}
			case INT :
				switch (fId) {
					case BYTE :
					case CHAR :
					case SHORT :
						return true;
					default :
						return false;
				}
			case SHORT :
				return (fId == BYTE);
		}
		return false;
	}
	
	public String getPrettySignature() {
		return NAMES[fId];
	}
}
