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
package org.eclipse.jdt.internal.corext.refactoring.participants;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.EnablementExpression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Expression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.ExpressionParser;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IElementHandler;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IVariablePool;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.StandardElementHandler;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TestResult;

public class ProcessorDescriptor {
	
	private IConfigurationElement fConfigurationElement;
	private static final ExpressionParser PARSER= new ExpressionParser(new IElementHandler[] { StandardElementHandler.getInstance() }); 

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String OVERRIDE= "override"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	public ProcessorDescriptor(IConfigurationElement element) {
		fConfigurationElement= element;
	}
	
	public String getId() {
		return fConfigurationElement.getAttribute(ID);
	}
	
	public boolean overrides() {
		return fConfigurationElement.getAttribute(OVERRIDE) != null;
	}
	
	public String getOverrideId() {
		return fConfigurationElement.getAttribute(OVERRIDE);
	}

	public boolean matches(IVariablePool pool) throws CoreException {
		Assert.isNotNull(pool);
		IConfigurationElement[] configElements= fConfigurationElement.getChildren(EnablementExpression.NAME);
		IConfigurationElement enablement= configElements.length > 0 ? configElements[0] : null; 
		if (enablement != null) {
			Expression exp= PARSER.parse(enablement);
			return (convert(exp.evaluate(pool)));
		}
		return false;
	}

	public IRefactoringProcessor createProcessor() throws CoreException {
		return (IRefactoringProcessor)fConfigurationElement.createExecutableExtension(CLASS);
	}
	
	private boolean convert(TestResult eval) {
		if (eval == TestResult.FALSE)
			return false;
		return true;
	}
	
	/*
	 * (non-Javadoc)
	 * For debugging purpose only
	 */
	public String toString() {
		return "Processor: " + getId(); //$NON-NLS-1$
	}
}
