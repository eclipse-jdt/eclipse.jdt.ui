/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.search.TypeNameMatch;

import org.eclipse.jdt.ui.dialogs.ITypeInfoRequestor;


public class TypeInfoRequestorAdapter implements ITypeInfoRequestor {

	private TypeNameMatch fMatch;

	public void setMatch(TypeNameMatch type) {
		fMatch= type;
	}

	@Override
	public String getEnclosingName() {
		return Signature.getQualifier(fMatch.getTypeQualifiedName());
	}

	@Override
	public int getModifiers() {
		return fMatch.getModifiers();
	}

	@Override
	public String getPackageName() {
		return fMatch.getPackageName();
	}

	@Override
	public String getTypeName() {
		return fMatch.getSimpleTypeName();
	}


}
