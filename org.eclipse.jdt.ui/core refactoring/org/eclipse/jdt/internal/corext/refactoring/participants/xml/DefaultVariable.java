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

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.internal.corext.Assert;

/**
 * A variable pool that can be used to add a new default variable
 * to a hierarchy of variable pools.
 * 
 * @since 3.0
 */
public final class DefaultVariable implements IVariablePool {

	private Object fDefaultVariable;
	private IVariablePool fParent;
	private IVariablePool fManagedPool;
	
	/**
	 * Constructs a new variable pool for a single default variable.
	 * 
	 * @param parent the pool's parent pool. Must not be <code>null</code>.
	 * @param defaultVariable the default variable
	 */
	public DefaultVariable(IVariablePool parent, Object defaultVariable) {
		Assert.isNotNull(parent);
		while (parent instanceof DefaultVariable) {
			parent= parent.getParent();
		}
		fManagedPool= parent;
		fDefaultVariable= defaultVariable;
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
		fManagedPool.addVariable(name, value);
	}

	/* (non-Javadoc)
	 * @see IVariablePool#removeVariable(java.lang.String)
	 */
	public Object removeVariable(String name) {
		return fManagedPool.removeVariable(name);
	}

	/* (non-Javadoc)
	 * @see IVariablePool#getVariable(java.lang.String)
	 */
	public Object getVariable(String name) {
		return fManagedPool.getVariable(name);
	}

	/* (non-Javadoc)
	 * @see IVariablePool#resolveVariable(java.lang.String, java.lang.Object[])
	 */
	public Object resolveVariable(String name, Object[] args) throws CoreException {
		return fManagedPool.resolveVariable(name, args);
	}
}
