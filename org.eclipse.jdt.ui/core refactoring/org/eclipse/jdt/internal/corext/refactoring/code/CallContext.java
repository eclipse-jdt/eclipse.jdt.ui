/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.code;

import java.util.List;

import org.eclipse.jdt.internal.corext.dom.ASTRewrite;

public class CallContext {

	public String[] expressions; 
	public ASTRewrite targetFactory; 
	public List usedCallerNames;
	public int callMode;

	public CallContext(String[] exp, ASTRewrite rewriter, List names, int cm) {
		super();
		expressions= exp;
		targetFactory= rewriter;
		usedCallerNames= names;
		callMode= cm;
	}
}
