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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A default variable pool implementation.
 * 
 * @since 3.0
 */
public class VariablePool implements IVariablePool {

	private IVariablePool fParent;
	private Object fDefaultVariable;
	private Map/*<String, Object>*/ fVariables;
	
	/**
	 * Create a new variable pool with the given parent and default
	 * variable.
	 * 
	 * @param parent the parent pool. Can be <code>null</code>.
	 * @param defaultVariable the default variable. Can be <code>null</code>.
	 */
	public VariablePool(IVariablePool parent, Object defaultVariable) {
		fParent= parent;
		fDefaultVariable= defaultVariable;
	}
	
	/**
	 * Create a new variable pool with the given parent, default
	 * variable and the variable stored under the name selection.
	 * 
	 * @param parent the parent pool. Can be <code>null</code>.
	 * @param defaultVariable the default variable. Can be <code>null</code>.
	 * @param selection the variable stored under the name selection. Can
	 *  be <code>null</code>
	 */
	public VariablePool(IVariablePool parent, Object defaultVariable, Object selection) {
		fParent= parent;
		fDefaultVariable= defaultVariable;
		if (selection != null)
			addVariable(SELECTION, selection);
	}
	
	/* (non-Javadoc)
	 * @see IVariablePool#getParent()
	 */
	public IVariablePool getParent() {
		return fParent;
	}
	
	/* (non-Javadoc)
	 * @see IVariablePool#getRoot()
	 */
	public IVariablePool getRoot() {
		if (fParent == null)
			return this;
		return fParent.getRoot();
	}
	
	/* (non-Javadoc)
	 * @see IVariablePool#getDefaultVariable()
	 */
	public Object getDefaultVariable() {
		return fDefaultVariable;
	}
	
	/* (non-Javadoc)
	 * @see IVariablePool#addVariable(java.lang.String, java.lang.Object)
	 */
	public void addVariable(String name, Object value) {
		Assert.isNotNull(name);
		Assert.isNotNull(value);
		if (fVariables == null)
			fVariables= new HashMap();
		fVariables.put(name, value);
	}
	
	/* (non-Javadoc)
	 * @see IVariablePool#removeVariable(java.lang.String)
	 */
	public Object removeVariable(String name) {
		Assert.isNotNull(name);
		if (fVariables == null)
			return null;
		return fVariables.remove(name);
	}
	
	/* (non-Javadoc)
	 * @see IVariablePool#getVariable(java.lang.String)
	 */
	public Object getVariable(String name) {
		Assert.isNotNull(name);
		if (SYSTEM.equals(name))
			return System.class;
		if (fVariables == null)
			return null;
		return fVariables.get(name);
	}
	
	/* (non-Javadoc)
	 * @IVariablePool#resolveVariable(java.lang.String, java.lang.Object[])
	 */
	public Object resolveVariable(String name, Object[] args) throws ExpressionException {
		if (PLUGIN_DESCRIPTOR.equals(name)) {
			if (args == null ||args.length != 1)
				throw new ExpressionException(
					ExpressionException.VARAIBLE_POOL_WRONG_NUMBER_OF_ARGUMENTS,
					ExpressionMessages.getFormattedString(
						"VariablePool.resolveVariable.arguments.wrong_number",  //$NON-NLS-1$
						new Object[] { "1", args == null ? "0" : ("" + args.length)})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (!(args[0] instanceof String)) 
				throw new ExpressionException(
					ExpressionException.VARAIBLE_POOL_ARGUMENT_IS_NOT_A_STRING,
					ExpressionMessages.getString("VariablePool.resolveVariable.arguments.not_a_string")); //$NON-NLS-1$
			return Platform.getPluginRegistry().getPluginDescriptor((String)(args[0]));
		}
		return null;
	}
}
