/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ltk.core.refactoring.participants;

import org.eclipse.ltk.internal.core.refactoring.Assert;

/**
 * Move arguments describes the data that a processor
 * provides to its move participants.
 * 
 * @since 3.0
 */
public class MoveArguments {
	
	private Object fTarget;
	private boolean fUpdateReferences;
	
	/**
	 * Creates new rename arguments.
	 * 
	 * @param target the target location of the move
	 * @param updateReferences <code>true</code> if reference
	 *  updating is requested; <code>false</code> otherwise
	 */
	public MoveArguments(Object target, boolean updateReferences) {
		Assert.isNotNull(target);
		fTarget= target;
		fUpdateReferences= updateReferences;
	}
	
	/**
	 * Returns the target location of the move
	 * 
	 * @return the move's target location
	 */
	public Object getTargetLocation() {
		return fTarget;
	}
	
	/**
	 * Returns whether reference updating is requested or not.
	 * 
	 * @return returns <code>true</code> if reference
	 *  updating is requested; <code>false</code> otherwise
	 */
	public boolean getUpdateReferences() {
		return fUpdateReferences;
	}
}
