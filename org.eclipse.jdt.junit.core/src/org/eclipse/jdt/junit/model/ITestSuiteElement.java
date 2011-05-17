/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.junit.model;

/**
 * Represents a test suite element.
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @since 3.3
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ITestSuiteElement extends ITestElementContainer {

	/**
	 * Returns the name of the suite. This is either the qualified type name of the
	 * suite class, or a custom name if one has been set.
	 * 
	 * @return the name of the suite
	 */
	public String getSuiteTypeName();

}
