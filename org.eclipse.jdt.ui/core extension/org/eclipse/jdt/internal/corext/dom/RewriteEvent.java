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
package org.eclipse.jdt.internal.corext.dom;


/**
 *
 */
public abstract class RewriteEvent {
		
	public static final int INSERTED= 1;
	public static final int REMOVED= 2;
	public static final int REPLACED= 4;
	public static final int CHILDREN_CHANGED= 8;
	
	public static final int UNCHANGED= 0;
	
	public abstract boolean isListRewrite();
	public abstract int getChangeKind();
	public abstract Object getOriginalValue();
	
}
