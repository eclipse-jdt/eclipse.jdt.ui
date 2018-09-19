/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.packageview;

import org.eclipse.jface.viewers.IElementComparer;

public class DefaultElementComparer implements IElementComparer {

	public static final DefaultElementComparer INSTANCE= new DefaultElementComparer();

	@Override
	public boolean equals(Object a, Object b) {
		return a.equals(b);
	}
	@Override
	public int hashCode(Object element) {
		return element.hashCode();
	}
}
