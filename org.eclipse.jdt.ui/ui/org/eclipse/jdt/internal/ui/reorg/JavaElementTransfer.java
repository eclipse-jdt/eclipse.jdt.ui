package org.eclipse.jdt.internal.ui.reorg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.corext.refactoring.Assert;

class JavaElementTransfer extends ByteArrayTransfer {

	/**
	 * Singleton instance.
	 */
	private static final JavaElementTransfer fgInstance = new JavaElementTransfer();
	
	// Create a unique ID to make sure that different Eclipse
	// applications use different "types" of <code>JavaElementTransfer</code>
	private static final String TYPE_NAME = "java-element-transfer-format:" + System.currentTimeMillis() + ":" + fgInstance.hashCode();//$NON-NLS-2$//$NON-NLS-1$
	
	private static final int TYPEID = registerType(TYPE_NAME);

	private JavaElementTransfer() {
	}
	
	/**
	 * Returns the singleton instance.
 	*
 	* @return the singleton instance
 	*/
	public static JavaElementTransfer getInstance() {
		return fgInstance;
	}

	/* (non-Javadoc)
	 * Method declared on Transfer.
	 */
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}
	
	/* (non-Javadoc)
	 * Returns the type names.
	 *
	 * @return the list of type names
	 */
	protected String[] getTypeNames() {
		return new String[] {TYPE_NAME};
	}

	/* (non-Javadoc)
	 * Method declared on Transfer.
	 */
	protected void javaToNative(Object data, TransferData transferData) {
		if (! canDataBeAccepted(data))
			return;
		IJavaElement[] sources = (IJavaElement[]) data;	

		/*
		 * The java element serialization format is:
		 *  (int) number of source references
		 * Then, the following for each source reference:
		 *  (String) java element handle identifier see <code>IJavaElement</code>
		 */
		int count= sources.length;
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream dataOut = new DataOutputStream(out);

			dataOut.writeInt(count);

			for (int i = 0; i < sources.length; i++) {
				writeJavaElement(dataOut, sources[i]);
			}

			dataOut.close();
			out.close();

			super.javaToNative(out.toByteArray(), transferData);
		} catch (IOException e) {
			//it's best to send nothing if there were problems
		} catch (JavaModelException e) {
			//it's best to send nothing if there were problems
		}		
	}

	/* (non-Javadoc)
	 * Method declared on Transfer.
	 */
	protected Object nativeToJava(TransferData transferData) {
	
		byte[] bytes = (byte[]) super.nativeToJava(transferData);
		if (bytes == null)
			return null;
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
		try {
			int count = in.readInt();
			IJavaElement[] results = new IJavaElement[count];
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
	
	private static boolean canDataBeAccepted(Object data){
		if (! (data instanceof IJavaElement[]))
			return false;
		return true;	
	}

	private static IJavaElement readJavaElement(DataInputStream dataIn) throws IOException {
		return (IJavaElement)JavaCore.create(dataIn.readUTF());
	}

	private static void writeJavaElement(DataOutputStream dataOut, IJavaElement sourceReference) throws IOException, JavaModelException {
		dataOut.writeUTF(((IJavaElement)sourceReference).getHandleIdentifier());
	}
}

