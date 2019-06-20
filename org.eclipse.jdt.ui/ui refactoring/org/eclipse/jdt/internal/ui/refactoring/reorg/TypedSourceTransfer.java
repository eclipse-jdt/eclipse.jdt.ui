/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.jdt.internal.ui.refactoring.reorg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jdt.internal.corext.refactoring.TypedSource;

public class TypedSourceTransfer extends ByteArrayTransfer {

	/**
	 * Singleton instance.
	 */
	private static final TypedSourceTransfer fgInstance = new TypedSourceTransfer();

	// Create a unique ID to make sure that different Eclipse
	// applications use different "types" of <code>TypedSourceTransfer</code>
	private static final String TYPE_NAME = "typed-source-transfer-format:" + System.currentTimeMillis() + ":" + fgInstance.hashCode();//$NON-NLS-2$//$NON-NLS-1$

	private static final int TYPEID = registerType(TYPE_NAME);

	private TypedSourceTransfer() {
	}

	/**
	 * Returns the singleton instance.
 	*
 	* @return the singleton instance
 	*/
	public static TypedSourceTransfer getInstance() {
		return fgInstance;
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] {TYPE_NAME};
	}

	@Override
	protected void javaToNative(Object data, TransferData transferData) {
		if (! (data instanceof TypedSource[]))
			return;
		TypedSource[] sources = (TypedSource[]) data;

		/*
		 * The serialization format is:
		 *  (int) number of elements
		 * Then, the following for each element:
		 *  (int) type (see <code>IJavaElement</code>)
		 *  (String) source of the element
		 */

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);

			dataOut.writeInt(sources.length);

			for (TypedSource source : sources) {
				writeJavaElement(dataOut, source);
			}

			dataOut.close();
			out.close();

			super.javaToNative(out.toByteArray(), transferData);
		} catch (IOException e) {
			//it's best to send nothing if there were problems
		}
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {

		byte[] bytes = (byte[]) super.nativeToJava(transferData);
		if (bytes == null)
			return null;
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
		try {
			int count = in.readInt();
			TypedSource[] results = new TypedSource[count];
			for (int i = 0; i < count; i++) {
				results[i] = readJavaElement(in);
				Assert.isNotNull(results[i]);
			}
			in.close();
			return results;
		} catch (IOException e) {
			return null;
		}
	}

	private static TypedSource readJavaElement(DataInputStream dataIn) throws IOException {
		int type= dataIn.readInt();
		String source= dataIn.readUTF();
		return TypedSource.create(source, type);
	}

	private static void writeJavaElement(DataOutputStream dataOut, TypedSource sourceReference) throws IOException {
		dataOut.writeInt(sourceReference.getType());
		dataOut.writeUTF(sourceReference.getSource());
	}
}

