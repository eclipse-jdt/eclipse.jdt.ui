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
package argument_out;

public class TestLocalReferencePrefix {
	public void main() {
		int a = 0;
		int b = 1;
		int c = 2;
		int d = 3;
    
		a = aa((a + ((b & c) | (~b & d))), 0) + b;
	}
	
	private int bb(int u, int v, int w) {
	  return (u & v) | (~u & w);
	}

	private int aa(int x, int n) {
	  return (x << n) | (x >>> (32 - n));
	}
}
