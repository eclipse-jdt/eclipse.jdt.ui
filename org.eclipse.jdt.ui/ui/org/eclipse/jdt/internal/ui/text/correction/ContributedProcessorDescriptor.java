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
package org.eclipse.jdt.internal.ui.text.correction;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Expression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.ExpressionParser;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.ITestResult;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class ContributedProcessorDescriptor {
	
	private IConfigurationElement fConfigurationElement;
	private Object fProcessorInstance;

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	private static final String SCOPE_STATE= "scopeState"; //$NON-NLS-1$

	public ContributedProcessorDescriptor(IConfigurationElement element) {
		fConfigurationElement= element;
		fProcessorInstance= null;
	}
			
	public IStatus checkSyntax() {
		String id= fConfigurationElement.getAttribute(ID);
		IConfigurationElement[] children= fConfigurationElement.getChildren(SCOPE_STATE);
		if (children.length > 1) {
			return new StatusInfo(IStatus.ERROR, "Only one <scopeState> element allowed. Disabling " + id); //$NON-NLS-1$
		}
		return new StatusInfo(IStatus.OK, "Syntactically correct quick assist/fix processor"); //$NON-NLS-1$
	}
	
	private boolean matches(ICompilationUnit scope) throws CoreException {
		IConfigurationElement[] children= fConfigurationElement.getChildren(SCOPE_STATE);
		if (children.length == 1) {
			ExpressionParser parser= ExpressionParser.getStandard();
			Expression expression= parser.parse(children[0]);
			if (expression.evaluate(scope) != ITestResult.TRUE) {
				return false;
			}
		}
		return true;
	}

	public Object getProcessor(ICompilationUnit scope) throws CoreException {
		if (matches(scope)) {
			if (fProcessorInstance == null) {
				fProcessorInstance= fConfigurationElement.createExecutableExtension(CLASS);
			}
			return fProcessorInstance;
		}
		return null;
	}
}
