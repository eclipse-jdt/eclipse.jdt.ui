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
class A
{
	public String bar2()
	{
		return "wee!";
	}
	public String bar3;
	
	public class Inner
	{
		
		public void bar0(){
			class Local{
				public void run()
				{
					System.out.println(bar2());
					bar3= "fred";
				}
			}
		}
		
		public void bar()
		{
			new Runnable()
			{
				public void run()
				{
					System.out.println(bar2());
					bar3= "fred";
				}
			};
		}
		
		class InnerInner{
			public void run()
			{
				System.out.println(bar2());
				bar3= "fred";
			}
		}
	}
}