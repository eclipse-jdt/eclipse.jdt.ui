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
package trycatch_in;

public class TestThenStatement {
	
 void foo() {
   TestThenStatement bar= null;
   if (bar != null)
	 /*]*/bar.run();/*[*/
 }
 
 void run() throws InterruptedException{
 }
}