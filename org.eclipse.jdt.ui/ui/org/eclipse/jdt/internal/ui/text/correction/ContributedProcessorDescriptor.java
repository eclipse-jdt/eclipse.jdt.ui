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

import java.util.Arrays;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.jdt.core.ICompilationUnit;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;

public class ContributedProcessorDescriptor {
	
	private IConfigurationElement fConfigurationElement;
	private Object fProcessorInstance;
	private ICompilationUnit fLastCUnit;
	private Boolean fStatus;
	private boolean fLastResult;

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	public ContributedProcessorDescriptor(IConfigurationElement element) {
		fConfigurationElement= element;
		fProcessorInstance= null;
		fLastCUnit= null;
		fStatus= null; // undefined
		if (fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT).length == 0) {
			fStatus= Boolean.TRUE;
		}
	}
			
	public IStatus checkSyntax() {
		IConfigurationElement[] children= fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT);
		if (children.length > 1) {
			String id= fConfigurationElement.getAttribute(ID);
			return new StatusInfo(IStatus.ERROR, "Only one <enablement> element allowed. Disabling " + id); //$NON-NLS-1$
		}
		return new StatusInfo(IStatus.OK, "Syntactically correct quick assist/fix processor"); //$NON-NLS-1$
	}
	
	private boolean matches(ICompilationUnit cunit) {
		if (fStatus != null) {
			return fStatus.booleanValue();
		}
		
		IConfigurationElement[] children= fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT);
		if (children.length == 1) {
			if (cunit.equals(fLastCUnit)) {
				return fLastResult;
			}
			try {
				ExpressionConverter parser= ExpressionConverter.getDefault();
				Expression expression= parser.perform(children[0]);
				EvaluationContext evalContext= new EvaluationContext(null, cunit);
				evalContext.addVariable("compilationUnit", cunit); //$NON-NLS-1$
				String[] natures= cunit.getJavaProject().getProject().getDescription().getNatureIds();
				evalContext.addVariable("projectNatures", Arrays.asList(natures)); //$NON-NLS-1$
	
				fLastResult= !(expression.evaluate(evalContext) != EvaluationResult.TRUE);
				fLastCUnit= cunit;
				return fLastResult;
			} catch (CoreException e) {
				JavaPlugin.log(e);
			}
		}
		fStatus= Boolean.FALSE;
		return false;
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
