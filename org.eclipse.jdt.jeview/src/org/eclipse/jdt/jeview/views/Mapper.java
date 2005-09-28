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

package org.eclipse.jdt.jeview.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class Mapper<I, O> {
	public abstract O map(I element);

	public static <I, O> List<O> build(Collection<? extends I> elements, Mapper<I, O> mapper) {
		ArrayList<O> result = new ArrayList<O>(elements.size());
		for (I element : elements)
			result.add(mapper.map(element));
		return result;
	}
	public List<O> mapToList(Collection<? extends I> elements) {
		ArrayList<O> result = new ArrayList<O>(elements.size());
		for (I element : elements)
			result.add(map(element));
		return result;
	}
}