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
import java.io.IOException;
import java.net.MalformedURLException;

public class D {
	public static class MyException extends Exception {
	}
	
	public volatile boolean flag;
	
	protected void foo() {
		int i= 10;
		try {
			try {
				if (flag)
					throw new IOException();
				if (!flag)
					throw new MyException();
			} catch (IOException e) {
			}
			i= 10;
		} catch (MyException e) {
		}
		read(i);
	}

	private void read(int i) {
	}	
}

