/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package generic;

import java.util.List;

public class TestAssignment<T, E extends T> {
	List<? extends T> lhs;
	List<E> rhs;
	{
		lhs= rhs;
	}
}
