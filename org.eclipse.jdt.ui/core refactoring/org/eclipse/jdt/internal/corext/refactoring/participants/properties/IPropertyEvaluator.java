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
package org.eclipse.jdt.internal.corext.refactoring.participants.properties;


public interface IPropertyEvaluator {
	
	static final int FALSE= 0;
	static final int TRUE= 1;
	static final int UNKNOWN= 2; 
	
	public int eval(Object o, String name, String value);
}
