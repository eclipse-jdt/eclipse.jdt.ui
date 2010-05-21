/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.indexsearch;

import java.util.HashSet;

public interface IIndex {

	void addRef(String word, String path);
	
	void remove(String path);
	
	void queryPrefix(HashSet result, String prefix);
}
