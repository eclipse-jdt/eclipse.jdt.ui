/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Here, an import is added for a type needed only after a qualification is added.
// 8, 19 -> 8, 29  removeAll == false
package p2;

import p1.A;

class InlineSite {
	Object thing= A.ALFRED;	
}