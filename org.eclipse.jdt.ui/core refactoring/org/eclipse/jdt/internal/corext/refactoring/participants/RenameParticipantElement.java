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

public class RenameParticipantElement {
	private IConfigurationElement fConfigurationElement;

	private static final String ID= "id"; //$NON-NLS-1$
	private static final String CLASS= "class"; //$NON-NLS-1$
	
	private static final String OBJECT_STATE= "objectState"; //$NON-NLS-1$

	public RenameParticipantElement(IConfigurationElement element) {
		fConfigurationElement= element;
	}
	
	public String getId() {
		return fConfigurationElement.getAttribute(ID);
	}
	
	public boolean matches(Object element) {
		IConfigurationElement objectState= fConfigurationElement.getChildren(OBJECT_STATE)[0];
		if (objectState != null) {
			Expression exp= new ObjectStateExpression(objectState);
			return exp.evaluate(element);
		}
		return true;
	}

	public IRenameParticipant createParticipant() throws CoreException {
		return (IRenameParticipant)fConfigurationElement.createExecutableExtension(CLASS);
	}
}
