/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.junit.launcher;

import java.util.Comparator;

public class ContainerComparator implements Comparator<String> {

	@Override
	public int compare(String container1, String container2) {
		if (container1 == null)
			container1= ""; //$NON-NLS-1$
		if (container2 == null)
			container2= ""; //$NON-NLS-1$
		return container1.compareTo(container2);
    }
}
