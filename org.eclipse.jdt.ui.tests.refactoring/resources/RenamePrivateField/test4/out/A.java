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
	 * @see #getYou()
	 * @see #setYou(int)
	 */
	private int fYou; //use getMe and setMe to update fMe
	
	public int getYou() {
		return fYou;
	}
	
	/** @param me stored into {@link #fYou}*/
	public void setYou(int me) {
		fYou= me;
	}
}