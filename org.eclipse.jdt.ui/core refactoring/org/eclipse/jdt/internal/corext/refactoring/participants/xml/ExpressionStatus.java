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
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.ui.JavaPlugin;

/**
 * Internal expression evaluation status.
 * 
 * @since 3.0
 */
public class ExpressionStatus extends Status implements IExpressionStatus {

	public ExpressionStatus(int severity, int code, String message) {
		this(severity, code, message, null);
	}
	
	public ExpressionStatus(int severity, int code, String message, Throwable exception) {
		super(severity, JavaPlugin.getPluginId(), code, message, exception);
	}
}
