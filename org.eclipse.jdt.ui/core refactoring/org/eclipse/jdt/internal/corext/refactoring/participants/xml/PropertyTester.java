/*******************************************************************************
 * Copyright (c) 2003 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.ui.JavaPlugin;


public abstract class PropertyTester implements IPropertyTester {
	
	protected static int convert(boolean value) {
		return value ? TRUE : FALSE;
	}
	
	protected static int testBoolean(String value, boolean result) {
		return (Boolean.valueOf(value).booleanValue()== result) ? TRUE : FALSE;
	}
	
	protected static int testShort(String value, short result) throws CoreException {
		try {
			return Short.parseShort(value) == result ? TRUE : FALSE;
		} catch (NumberFormatException e) {
			throw new CoreException(new Status(
				IStatus.ERROR,
				JavaPlugin.getPluginId(),
				// TODO Have to define error codes. 
				IStatus.ERROR,
				"Can't convert \"" + value + "\" into a short.",
				e));
		}
	}
}
