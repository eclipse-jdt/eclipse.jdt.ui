/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.nls.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class LineReader extends Object {
	protected static final int fgLF= '\n';
	protected static final int fgCR= '\r';

	private BufferedReader fReader;

	protected int fPushbackChar;
	protected boolean fPushback;

	public LineReader(InputStream in) {
		this(new InputStreamReader(in));
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
			if (ch == fgLF)
				return 1;
			if (ch == fgCR) {
				ch= fReader.read();
				if (ch == fgLF)
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

	public void close() throws IOException {
		fReader.close();
	}
}