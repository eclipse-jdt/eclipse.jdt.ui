/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.tagging;

/**
 * Interface implemented by processors able to rename derived elements.
 * 
 * @since 3.2
 */
public interface IDerivedElementUpdating {

	/**
	 * Checks if this refactoring object is capable of updating derived elements
	 * of the renamed element.
	 */
	public boolean canEnableDerivedElementUpdating();

	/**
	 * If <code>canEnableDerivedElementUpdating</code> returns
	 * <code>true</code>, then this method is used to inform the refactoring
	 * object whether derived elements should be updated. This call can be
	 * ignored if <code>canEnableDerivedElementUpdating</code> returns
	 * <code>false</code>.
	 */
	public void setUpdateDerivedElements(boolean update);

	/**
	 * If <code>canEnableDerivedElementUpdating</code> returns
	 * <code>true</code>, then this method is used to ask the refactoring
	 * object whether derived elements should be updated. This call can be
	 * ignored if <code>canEnableDerivedElementUpdating</code> returns
	 * <code>false</code>.
	 */
	public boolean getUpdateDerivedElements();

	/**
	 * If <code>canEnableDerivedElementUpdating</code> returns
	 * <code>true</code>, then this method is used to set the match strategy
	 * for determining derived elements.
	 * 
	 * @param selectedStrategy one of the STRATEGY_* constants in {@link org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor}
	 */
	public void setMatchStrategy(int selectedStrategy);

	/**
	 * If <code>canEnableDerivedElementUpdating</code> returns
	 * <code>true</code>, then this method is used to ask the refactoring
	 * object which match strategy is used for determining derived elements.
	 * 
	 * @return one of the STRATEGY_* constants in {@link org.eclipse.jdt.internal.corext.refactoring.rename.RenamingNameSuggestor}
	 */
	public int getMatchStrategy();

}
