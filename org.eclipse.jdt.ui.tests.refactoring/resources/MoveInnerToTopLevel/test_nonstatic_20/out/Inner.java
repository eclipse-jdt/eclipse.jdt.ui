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
class Inner{
	/** Comment */
	private A a;
	Inner(A a){
		super();
		this.a= a;
	}
	Inner(A a, int i){
		this(a);
	}
	Inner(A a, boolean b){
		this(a, 1);
	}
}