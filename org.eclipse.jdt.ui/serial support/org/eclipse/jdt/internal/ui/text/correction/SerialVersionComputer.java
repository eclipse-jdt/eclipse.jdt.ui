/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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

	/** The serial version computation error prefix */
	public static final String ERROR_PREFIX= "SerialVersionComputationError: "; //$NON-NLS-1$

	/**
	 * The entry point of this process.
	 * 
	 * @param arguments The arguments to pass
	 */
	public static void main(final String[] arguments) {
		if (arguments.length > 0) {
			try {
				final ObjectStreamClass stream= ObjectStreamClass.lookup(Class.forName(arguments[0]));
				if (stream != null) {
					System.out.println(ERROR_PREFIX + stream.getSerialVersionUID());
					return;
				} else
					System.err.println(ERROR_PREFIX + SerialVersionMessages.getFormattedString("SerialVersionComputer.not.serializable", arguments[0])); //$NON-NLS-1$
			} catch (ClassNotFoundException exception) {
				System.err.println(ERROR_PREFIX + SerialVersionMessages.getFormattedString("SerialVersionComputer.not.resolvable", arguments[0])); //$NON-NLS-1$
			}
		} else
			System.err.println(ERROR_PREFIX + SerialVersionMessages.getString("SerialVersionComputer.no.argument")); //$NON-NLS-1$
		System.out.println(1);
	}
}