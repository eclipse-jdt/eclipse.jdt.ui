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

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class ContributedProcessorDescriptor {
	
	private IConfigurationElement fConfigurationElement;
	private Object fProcessorInstance;
	private ICompilationUnit fLastCUnit;
	private boolean fLastResult;

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	public ContributedProcessorDescriptor(IConfigurationElement element) {
		fConfigurationElement= element;
		fProcessorInstance= null;
		fLastCUnit= null;
	}
			
	public IStatus checkSyntax() {
		String id= fConfigurationElement.getAttribute(ID);
		IConfigurationElement[] children= fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT);
		if (children.length > 1) {
			return new StatusInfo(IStatus.ERROR, "Only one <enablement> element allowed. Disabling " + id); //$NON-NLS-1$
		}
		return new StatusInfo(IStatus.OK, "Syntactically correct quick assist/fix processor"); //$NON-NLS-1$
	}
	
	private boolean matches(ICompilationUnit cunit) throws CoreException {
		IConfigurationElement[] children= fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT);
		if (children.length == 1) {
			if (cunit.equals(fLastCUnit)) {
				return fLastResult;
			}
			
			ExpressionConverter parser= ExpressionConverter.getDefault();
			Expression expression= parser.perform(children[0]);
			EvaluationContext evalContext= new EvaluationContext(null, cunit);
			evalContext.addVariable("selection", cunit); //$NON-NLS-1$
			fLastResult= !(expression.evaluate(evalContext) != EvaluationResult.TRUE);
			fLastCUnit= cunit;
			return fLastResult;

		}
		return true;
	}

	public Object getProcessor(ICompilationUnit cunit) throws CoreException {
		if (matches(cunit)) {
			if (fProcessorInstance == null) {
				fProcessorInstance= fConfigurationElement.createExecutableExtension(CLASS);
			}
			return fProcessorInstance;
		}
		return null;
	}
}
