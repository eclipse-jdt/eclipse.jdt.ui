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
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Expression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.ExpressionParser;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IElementHandler;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Scope;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.SelectionExpression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.StandardElementHandler;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TestResult;

public class ProcessorDescriptor {
	
	private IConfigurationElement fConfigurationElement;
	private static final ExpressionParser PARSER= new ExpressionParser(new IElementHandler[] { StandardElementHandler.getInstance() }); 

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String OVERRIDE= "override"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	private static final String OBJECT_STATE= "objectState"; //$NON-NLS-1$

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

	public boolean matches(Object[] elements) throws CoreException {
		Assert.isNotNull(elements);
		IConfigurationElement[] configElements= fConfigurationElement.getChildren(SelectionExpression.NAME);
		IConfigurationElement selectionState= configElements.length > 0 ? configElements[0] : null; 
		if (selectionState != null) {
			Expression exp= PARSER.parse(selectionState);
			return (convert(exp.evaluate(new Scope(null, elements))));
		} else if (elements.length == 1) {
			IConfigurationElement objectState= fConfigurationElement.getChildren(OBJECT_STATE)[0];
			if (objectState != null) {
				Expression exp= PARSER.parse(objectState);
				return (convert(exp.evaluate(new Scope(null, elements[0]))));
			}
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
}
