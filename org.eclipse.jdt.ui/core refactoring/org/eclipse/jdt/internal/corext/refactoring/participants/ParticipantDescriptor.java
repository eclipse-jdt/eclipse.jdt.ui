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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.corext.Assert;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.EnablementExpression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.Expression;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.ExpressionParser;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IElementHandler;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.IVariablePool;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.StandardElementHandler;
import org.eclipse.jdt.internal.corext.refactoring.participants.xml.TestResult;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class ParticipantDescriptor {
	
	private IConfigurationElement fConfigurationElement;

	private static final ExpressionParser EXPRESSION_PARSER= 
		new ExpressionParser(new IElementHandler[] { StandardElementHandler.getInstance() }); 

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	public ParticipantDescriptor(IConfigurationElement element) {
		fConfigurationElement= element;
	}
	
	public String getId() {
		return fConfigurationElement.getAttribute(ID);
	}
	
	public IStatus checkSyntax() {
//		IConfigurationElement[] children= fConfigurationElement.getChildren(SCOPE_STATE);
//		switch(children.length) {
//			case 0:
//				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
//					"Mandantory element <scopeState> missing. Disabling rename participant " + getId(), null);
//			case 1:
//				break;
//			default:
//				return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
//					"Only one <scopeState> element allowed. Disabling rename participant " + getId(), null);
//		}
//		children= fConfigurationElement.getChildren(OBJECT_STATE);
//		if (children.length > 1) {
//			return new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
//				"Only one <objectState> element allowed. Disabling rename participant " + getId(), null);
//		}
		return new Status(IStatus.OK, JavaPlugin.getPluginId(), IStatus.OK, 
			"Syntactically correct rename participant element", null);
	}
	
	public boolean matches(IVariablePool pool) throws CoreException {
		IConfigurationElement[] elements= fConfigurationElement.getChildren(EnablementExpression.NAME);
		if (elements.length == 0)
			return false;
		Assert.isTrue(elements.length == 1);
		Expression exp= EXPRESSION_PARSER.parse(elements[0]);
		return convert(exp.evaluate(pool));
	}

	public IRefactoringParticipant createParticipant() throws CoreException {
		return (IRefactoringParticipant)fConfigurationElement.createExecutableExtension(CLASS);
	}
	
	private boolean convert(TestResult eval) {
		if (eval == TestResult.FALSE)
			return false;
		return true;
	}
}
