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
class A{
	/**
	 * @see #getMe()
	 * @see #setMe(int)
	 */
	private int fMe; //use getMe and setMe to update fMe
	
	public int getMe() {
		return fMe;
	}
	
	/** @param me stored into {@link #fMe}*/
	public void setMe(int me) {
		fMe= me;
	}
}