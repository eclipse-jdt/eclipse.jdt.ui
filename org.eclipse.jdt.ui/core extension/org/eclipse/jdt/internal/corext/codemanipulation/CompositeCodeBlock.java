/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.corext.codemanipulation;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;

public class CompositeCodeBlock extends AbstractCodeBlock {

	private List fBlocks= new ArrayList(3);

	public void add(AbstractCodeBlock block) {
		fBlocks.add(block);
	}

	public boolean isEmpty() {
		return fBlocks.isEmpty();
	}

	public void fill(StringBuffer buffer, String firstLineIndent, String indent, String lineSeparator) throws CoreException {
		int size= fBlocks.size();
		int lastBlock= size - 1;
		int inserted= 0;
		for (int i= 0; i < size; i++) {
			AbstractCodeBlock block= (AbstractCodeBlock)fBlocks.get(i);
			if (block.isEmpty())
				continue;
			if (inserted == 0)
				block.fill(buffer, firstLineIndent, indent, lineSeparator);
			else
				block.fill(buffer, indent, indent, lineSeparator);
			inserted++;
			if (i < lastBlock)
				buffer.append(lineSeparator);
		}
	}
}
