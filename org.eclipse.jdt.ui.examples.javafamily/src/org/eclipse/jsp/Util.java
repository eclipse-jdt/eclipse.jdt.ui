/*******************************************************************************
 * Copyright (c) 2003, 2025 IBM Corporation and others.
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
package org.eclipse.jsp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Util {

	static char[] getChars(String s) {
		int l= s.length();
		char[] cc= new char[l];
		if (l > 0)
			s.getChars(0, l, cc, 0);
		return cc;
	}

	static char[] getFileCharContent(File file, String encoding) throws IOException {
		System.out.println("****jdt.internal.compiler.util.Util.getFileCharContent: " + file + " " + encoding); //$NON-NLS-1$ //$NON-NLS-2$
		try (InputStream stream= new BufferedInputStream(new FileInputStream(file))) {
			return Util.getInputStreamAsCharArray(stream, (int) file.length(), encoding);
		}
	}

	public static char[] getInputStreamAsCharArray(InputStream stream, int length, String encoding) throws IOException {
		try (InputStreamReader reader= (encoding == null)
			? new InputStreamReader(stream)
			: new InputStreamReader(stream, encoding)) {
			char[] contents;
			if (length == -1) {
				contents = new char[0];
				int contentsLength = 0;
				int amountRead = -1;
				do {
					int amountRequested = Math.max(stream.available(), 8192);  // read at least 8K

					// resize contents if needed
					if (contentsLength + amountRequested > contents.length) {
						System.arraycopy(
								contents,
								0,
								contents = new char[contentsLength + amountRequested],
								0,
								contentsLength);
					}

					// read as many chars as possible
					amountRead = reader.read(contents, contentsLength, amountRequested);

					if (amountRead > 0) {
						// remember length of contents
						contentsLength += amountRead;
					}
				} while (amountRead != -1);

				// resize contents if necessary
				if (contentsLength < contents.length) {
					System.arraycopy(
							contents,
							0,
							contents = new char[contentsLength],
							0,
							contentsLength);
				}
			} else {
				contents = new char[length];
				int len = 0;
				int readSize = 0;
				while ((readSize != -1) && (len != length)) {
					// See PR 1FMS89U
					// We record first the read size. In this case len is the actual read size.
					len += readSize;
					readSize = reader.read(contents, len, length - len);
				}
				// See PR 1FMS89U
				// Now we need to resize in case the default encoding used more than one byte for each
				// character
				if (len != length)
					System.arraycopy(contents, 0, (contents = new char[len]), 0, len);
			}

			return contents;
		}
	}
}
