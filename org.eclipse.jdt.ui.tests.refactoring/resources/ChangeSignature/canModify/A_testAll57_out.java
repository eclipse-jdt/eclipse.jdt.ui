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
package p; //swap hello and goodbye

class TEST
{
   public interface X
   {
	  public void method(final int i, final String goodbye, String hello);
   }
   
   private static X x = new X()
	{
	   public void method(final int i, final String goodbye, String hello)
	   {
		  System.err.println(hello + goodbye);
	   }
	};
   
   public static void main(String[] args)
   {
	  x.method(1, "goodbye", "hello");
   }
}
