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
package receiver_in;

public class TestExplicitThisMethodReceiver {
	protected Logger4 getLogger() {
		return Logger.getLogger("");
	}
    protected Logger4 getLOG() {
        return getLogger();
    }
    public void ref() {
    	this./*]*/getLOG()/*[*/.info("message");
    }
}

class Logger4 {
	public static Logger4 getLogger(String string) {
		return null;
	}
	public void info(String string) {
	}
}