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
package org.eclipse.jdt.internal.corext.refactoring.reorg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;

public class JavaElementTransfer extends ByteArrayTransfer {

	/**
	 * Singleton instance.
	 */
	private static final JavaElementTransfer fInstance= new JavaElementTransfer();

	// Create a unique ID to make sure that different Eclipse
	// applications use different "types" of <code>JavaElementTransfer</code>
	private static final String TYPE_NAME= "java-element-transfer-format:" + System.currentTimeMillis() + ":" + fInstance.hashCode(); //$NON-NLS-2$//$NON-NLS-1$

	private static final int TYPEID= registerType(TYPE_NAME);

	private JavaElementTransfer() {
	}

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static JavaElementTransfer getInstance() {
		return fInstance;
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

	@Override
	protected void javaToNative(Object data, TransferData transferData) {
		if (!(data instanceof IJavaElement[]))
			return;

		IJavaElement[] javaElements= (IJavaElement[]) data;
		/*
		 * The element serialization format is:
		 *  (int) number of element
		 * Then, the following for each element:
		 *  (String) handle identifier
		 */

		try (ByteArrayOutputStream out= new ByteArrayOutputStream();
				DataOutputStream dataOut= new DataOutputStream(out)) {

			//write the number of elements
			dataOut.writeInt(javaElements.length);

			//write each element
			for (IJavaElement javaElement : javaElements) {
				writeJavaElement(dataOut, javaElement);
			}

			//cleanup
			byte[] bytes= out.toByteArray();
			super.javaToNative(bytes, transferData);
		} catch (IOException e) {
			//it's best to send nothing if there were problems
		}
	}

	@Override
	protected Object nativeToJava(TransferData transferData) {
		/*
		 * The element serialization format is:
		 *  (int) number of element
		 * Then, the following for each element:
		 *  (String) handle identifier
		 */

		byte[] bytes= (byte[]) super.nativeToJava(transferData);
		if (bytes == null)
			return null;
		DataInputStream in= new DataInputStream(new ByteArrayInputStream(bytes));
		try {
			int count= in.readInt();
			IJavaElement[] results= new IJavaElement[count];
			for (int i= 0; i < count; i++) {
				results[i]= readJavaElement(in);
			}
			return results;
		} catch (IOException e) {
			return null;
		}
	}

	private IJavaElement readJavaElement(DataInputStream dataIn) throws IOException {
		String handleIdentifier= dataIn.readUTF();
		return JavaCore.create(handleIdentifier);
	}

	private static void writeJavaElement(DataOutputStream dataOut, IJavaElement element) throws IOException {
		dataOut.writeUTF(element.getHandleIdentifier());
	}
}
