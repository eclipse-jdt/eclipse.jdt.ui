/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;


class LineReader implements AutoCloseable {
	protected static final int LF= '\n';
	protected static final int CR= '\r';

	private BufferedReader fReader;

	protected int fPushbackChar;
	protected boolean fPushback;

	public LineReader(InputStream in, String encoding) throws IOException {
		this(new InputStreamReader(in, encoding));
	}

	public LineReader(Reader reader) {
		fPushback= false;
		fReader= new BufferedReader(reader);
	}

	public int readLine(StringBuffer sb) throws IOException {
		int ch= -1;
		sb.setLength(0);
		if (fPushback) {
			ch= fPushbackChar;
			fPushback= false;
		} else
			ch= fReader.read();
		while (ch >= 0) {
			if (ch == LF)
				return 1;
			if (ch == CR) {
				ch= fReader.read();
				if (ch == LF)
					return 2;
				else {
					fPushbackChar= ch;
					fPushback= true;
					return 1;
				}
			}
			sb.append((char) ch);
			ch= fReader.read();
		}
		return -1;
	}

	@Override
	public void close() throws IOException {
		fReader.close();
	}
}
