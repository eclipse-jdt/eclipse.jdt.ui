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
package p;
import java.util.*;
class A {
	/**
	 * @param really TODO
	 * @param f 1st param of {@link A#getList(boolean, int, char) me}
	 * @param t
	 * @see getList
	 * @see #getList(boolean, int, char)
	 * @see #getList(int from, tho long)
	 * @see #getList(boolean really, int f, char t)
	 * @return list
	 * @param bogus{@link #getList}
	 */
	public List getList(boolean really, int f, char t) {
		return new ArrayList((int)t-f);
	}
	
	/** start here
	 * Doesn't call {@linkplain #getList(boolean, int, char)}
	 *
	 * @see getList
	 * @see #getList
	 * @see A#getList(boolean, int, char)
	 * @see A#getList(int, long, Object[][])
	 * @see A#getList (
	 *   boolean really,int f, char t
	 * )
	 * @see #getList(..)
	 * @see p.A#getList(int, int, boolean)
	 * @see <a href="spec.html#section">Java Spec</a>
	 * @see A# getList(boolean, int, char)
	 */
	public ArrayList getList(int from, long to, Object[] arr[]) {
		return new ArrayList(Arrays.asList(arr).subList(from, (int)to));
	}
}
