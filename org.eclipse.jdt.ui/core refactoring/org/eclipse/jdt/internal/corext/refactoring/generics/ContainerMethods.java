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

package org.eclipse.jdt.internal.corext.refactoring.generics;

import org.eclipse.jdt.core.dom.IMethodBinding;

public class ContainerMethods {

	private final AugmentRawContainerClientsTCFactory fTCFactory;

	public ContainerMethods(AugmentRawContainerClientsTCFactory factory) {
		fTCFactory= factory;
		
		
	}

	public SpecialMethod getSpecialMethodFor(IMethodBinding methodBinding) {
		// TODO Auto-generated method stub
		return null;
	}

}
