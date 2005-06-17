/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.io.ObjectStreamClass;

/**
 * Support class to compute the serial version ID in a separate VM.
 * <p>
 * To use this class for the computation of a serial version ID, the following steps have to be performed:
 * <ul>
 * <li>Create a new VM configuration corresponding to the one that created the class whose serial version ID has to be computed</li>
 * <li>Set up the class path for the new VM</li>
 * <li>Set up the command line. The only argument to pass is the fully qualified name of the class to compute the ID for.</li>
 * <li>Launch the configured VM</li>
 * <li>Listen on the standard output stream for the result of the computation</li>
 * <li>Listen on the standard error stream for eventual errors</li>
 * </ul>
 * 
 * @since 3.1
 */
public final class SerialVersionComputer {

	/** The serial version computation error postfix */
	public static final String ERROR_POSTFIX= "__SerialVersionComputationErrorPostfix__"; //$NON-NLS-1$

	/** The serial version computation error prefix */
	public static final String ERROR_PREFIX= "__SerialVersionComputationErrorPrefix__"; //$NON-NLS-1$

	/** The serial version computation result postfix */
	public static final String RESULT_POSTFIX= "__SerialVersionComputationResultPostfix__"; //$NON-NLS-1$

	/** The serial version computation result prefix */
	public static final String RESULT_PREFIX= "__SerialVersionComputationResultPrefix__"; //$NON-NLS-1$

	/**
	 * The entry point of this process.
	 * 
	 * @param arguments The arguments to pass
	 */
	public static void main(final String[] arguments) {
		boolean success= false;
		try {
			if (arguments.length > 0) {
				try {
					final ObjectStreamClass stream= ObjectStreamClass.lookup(Class.forName(arguments[0]));
					if (stream != null) {
						System.out.println(RESULT_PREFIX + String.valueOf(stream.getSerialVersionUID()) + RESULT_POSTFIX);
						success= true;
					} else
						System.err.println(ERROR_PREFIX + SerialVersionMessages.getFormattedString("SerialVersionComputer.not.serializable", arguments[0]) + ERROR_POSTFIX); //$NON-NLS-1$
				} catch (ClassNotFoundException exception) {
					System.err.println(ERROR_PREFIX + SerialVersionMessages.getFormattedString("SerialVersionComputer.not.resolvable", arguments[0]) + ERROR_POSTFIX); //$NON-NLS-1$
				}
			} else
				System.err.println(ERROR_PREFIX + SerialVersionMessages.getString("SerialVersionComputer.no.argument") + ERROR_POSTFIX); //$NON-NLS-1$
		} finally {
			if (!success)
				System.out.println(RESULT_PREFIX + String.valueOf(1) + RESULT_POSTFIX);
		}
	}
}
