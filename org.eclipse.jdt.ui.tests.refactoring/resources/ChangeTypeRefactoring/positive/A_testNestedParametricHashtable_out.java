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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

public class A_testNestedParametricHashtable_in {
	void foo(){
		Dictionary<String, Vector<Integer>> h = new Hashtable<String, Vector<Integer>>();
	}
}
