package org.eclipse.jdt.internal.ui;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * An exception to wrap a status. This is necessary to use the core's IRunnableWithProgress
 * support
 */

public class JavaUIException extends CoreException {
	
	public JavaUIException(IStatus status) {
		super(status);
	}	
}