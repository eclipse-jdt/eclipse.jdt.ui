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

package org.eclipse.jdt.internal.corext.refactoring.genericize;

import org.eclipse.jdt.internal.corext.refactoring.typeconstraints2.TypeConstraintFactory2;


public class AugmentRawTypesTCFactory extends TypeConstraintFactory2 {

	//TODO: filter makeDeclaringTypeVariable, since that's not used?
	//-> would need to adapt create...Constraint methods to deal with null
	
}
