/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.junit.wizards;

/**
 * Interface to notify an page that the operation
 * is about to run. This gives the page a chance
 * to copy widget data into fields so that the
 * data can be access from a ModalContextThread
 */
public interface IAboutToRunOperation {

	void aboutToRunOperation();

}
