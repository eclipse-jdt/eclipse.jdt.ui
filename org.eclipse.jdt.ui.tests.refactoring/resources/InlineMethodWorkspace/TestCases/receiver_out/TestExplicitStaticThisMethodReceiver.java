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
package receiver_out;

public class TestExplicitStaticThisMethodReceiver {
    static Logger3 getLogger() {
    	return Logger3.getLogger("");
    }
    protected Logger3 getLOG() {
        return getLogger();
    }
    public void ref() {
    	getLogger().info("message");
    }
}

class Logger3 {
	public static Logger3 getLogger(String string) {
		return null;
	}
	public void info(String string) {
	}
}