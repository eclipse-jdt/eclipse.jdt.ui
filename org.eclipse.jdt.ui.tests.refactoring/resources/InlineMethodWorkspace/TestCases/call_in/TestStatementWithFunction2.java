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
package call_in;

class TestStatementWithFunction2 {
    public void main(){
       /*]*/foo();/*[*/
    }
    
    public int foo(){
        return bar();
    }
    public int bar() {
    	System.out.println("Bar called");
    	return 10;
    }
}
