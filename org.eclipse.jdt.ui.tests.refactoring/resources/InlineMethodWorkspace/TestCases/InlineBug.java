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
import java.io.Writer;

public class InlineBug
{
	public void caller(Writer out) throws Exception
	{
		out.write("start caller");
		inlineMe(out);
		out.write("end caller");
	}

	public void inlineMe(Writer out) throws Exception
	{
		out.write("start render");
		subroutine(true, out);
		out.write("end render");
	}

	void subroutine(boolean isSelected, Writer out) throws Exception
	{
		if (isSelected)
		{
			out.write("selected");
		}
	}
}
