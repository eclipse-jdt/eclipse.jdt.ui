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
package org.eclipse.jdt.internal.corext.refactoring.participants.xml;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.corext.Assert;


public class ExpressionParser {
	
	private IElementHandler[] fHandlers;
	private static final ExpressionParser INSTANCE= new ExpressionParser( 
		new IElementHandler[] { StandardElementHandler.getInstance() } ); 
	
	public static ExpressionParser getStandard() {
		return INSTANCE;
	}
	
	public ExpressionParser(IElementHandler[] handlers) {
		Assert.isNotNull(handlers);
		fHandlers= handlers;
	}
	
	public Expression parse(IConfigurationElement element) throws CoreException {
		for (int i= 0; i < fHandlers.length; i++) {
			IElementHandler handler= fHandlers[i];
			Expression result= handler.create(element, this);
			if (result != null)
				return result;
		}
		return null;
	}
	
	public void processChildren(CompositeExpression result, IConfigurationElement element) throws CoreException {
		IConfigurationElement[] children= element.getChildren();
		if (children != null) {
			for (int i= 0; i < children.length; i++) {
				Expression child= parse(children[i]);
				if (child == null)
					throw new CoreException(new Status(IStatus.ERROR, JavaPlugin.getPluginId(),
						IStatus.ERROR, "Unknown expression element", null));
				result.add(child);
			}
		}		
	}
}
