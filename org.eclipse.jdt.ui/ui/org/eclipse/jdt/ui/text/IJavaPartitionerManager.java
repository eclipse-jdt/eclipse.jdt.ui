/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * This is an implementation of an early-draft specification developed under the Java
 * Community Process (JCP) and is made available for testing and evaluation purposes
 * only. The code is not compatible with any specification of the JCP.
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.text;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;

/**
 * This IDocumentPartitioner Manager manages the creation of the IDocumentPartitioner 
 * and the corresponding IPartitionTokenScanner.
 * 
 * @since 3.19
 */
public interface IJavaPartitionerManager {

	/**
	 * Returns a scanner which is configured to scan
	 * Java-specific partitions, which are multi-line comments,
	 * Javadoc comments, and regular Java source code.
	 *
	 * @return a Java partition scanner
	 */
	public IPartitionTokenScanner getPartitionScanner();

	/**
	 * Factory method for creating a Java-specific document partitioner
	 * using the partitions scanner. This method is a
	 * convenience method.
	 *
	 * @return a newly created Java document partitioner
	 */
	public IDocumentPartitioner createDocumentPartitioner();
}
