/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.text.correction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectStreamClass;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;		// do not convert to ICU, class runs in separate VM
import java.text.MessageFormat;	// do not convert to ICU, class runs in separate VM
import java.util.Date;

/**
 * Support class to compute the serial version ID in a separate VM.
 * <p>
 * To use this class for the computation of a serial version ID, the following
 * steps have to be performed:
 * <ul>
 * <li>Create a new VM configuration corresponding to the one that created the
 * class whose serial version ID has to be computed</li>
 * <li>Set up the class path for the new VM</li>
 * <li>Set up the command line. The only arguments to pass are the fully
 * qualified names of the classes to compute the IDs for.</li>
 * <li>Launch the configured VM</li>
 * <li>Read the results from the serial IDs temp file</li>
 * </ul>
 * 
 * @since 3.1
 */
public final class SerialVersionComputer {

	private static final String NON_RESOLVABLE_CLASS= "The class {0} could not be resolved."; //$NON-NLS-1$
	private static final String NON_SERIALIZABLE_CLASS= "The class {0} does not implement ''java.io.Serializable'' or ''java.io.Externalizable'' or has already an id"; //$NON-NLS-1$

	/**
	 * Should the process be debugged? (adapt the path of the log file
	 * accordingly)
	 */
	private static final boolean DEBUG= false;

	/** The temp file encoding */
	private static final String TEMP_FILE_ENCODING= "utf-8"; //$NON-NLS-1$

	/** The temp file name */
	private static final String TEMP_FILE_NAME= "serials.tmp"; //$NON-NLS-1$

	/**
	 * The entry point of this process.
	 * 
	 * @param arguments
	 *            The arguments to pass
	 */
	public static void main(final String[] arguments) {
		BufferedWriter logger= null;
		if (DEBUG) {
			try {
				logger= new BufferedWriter(new OutputStreamWriter(new FileOutputStream("C:\\serial.log"))); //$NON-NLS-1$
				final Date date= new Date(System.currentTimeMillis());
				logger.write("Begin Session: " + DateFormat.getDateInstance().format(date) + " at " + DateFormat.getTimeInstance().format(date) + "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				logger.write("Argument Count: " + arguments.length + "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
			} catch (IOException exception) {
				// Do nothing
			}
		}
		if (arguments.length > 0) {
			final String directory= System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
			if (directory != null && !"".equals(directory)) { //$NON-NLS-1$
				final String separator= System.getProperty("file.separator"); //$NON-NLS-1$
				if (separator != null && !"".equals(separator)) { //$NON-NLS-1$
					final File file= new File(directory + separator + TEMP_FILE_NAME);
					if (DEBUG) {
						try {
							logger.write("Created file: " + file.getCanonicalPath() + "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
						} catch (IOException exception) {
							// Do nothing
						}
					}
					Writer writer= null;
					try {
						file.delete();
						file.createNewFile();
						writer= new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), TEMP_FILE_ENCODING));
						for (int i= 0; i < arguments.length; i++) {
							try {
								final ObjectStreamClass clazz= ObjectStreamClass.lookup(Class.forName(arguments[i]));
								if (clazz != null) {
									writer.write(new Long(clazz.getSerialVersionUID()).toString());
									writer.write('\n');
								} else {
									writer.write(format(NON_SERIALIZABLE_CLASS, arguments[i]));
									writer.write('\n');
								}
							} catch (ClassNotFoundException exception) {
								writer.write(format(NON_RESOLVABLE_CLASS, arguments[i]));
								writer.write('\n');
							}
						}
					} catch (Throwable throwable) {
						if (DEBUG) {
							PrintWriter printer= null;
							try {
								logger.write("Exception occurred: " + throwable.getLocalizedMessage() + "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
								printer= new PrintWriter(logger);
								throwable.printStackTrace(printer);
							} catch (IOException exc) {
								// Do nothing
							} finally {
								if (printer != null) {
									printer.close();
								}
							}
						}
					} finally {
						if (writer != null) {
							try {
								writer.close();
							} catch (IOException exception) {
								// Do nothing
							}
						}
					}
				}
			}
		}
		if (DEBUG) {
			try {
				logger.write("End Session\r\n"); //$NON-NLS-1$
				logger.close();
			} catch (IOException exception) {
				// Do nothing
			}
		}
	}
	
	private static String format(String message, Object object) {
		return MessageFormat.format(message, new Object[] { object});
	}
}
