/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.xml;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TypeExtender;


public class I_TypeExtender extends TypeExtender {

	public Object invoke(Object receiver, String method, Object[] args) {
		if ("ordering".equals(method)) {
			return "I";
		}
		Assert.isTrue(false);
		return null;
	}
}
