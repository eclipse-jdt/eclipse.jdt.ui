/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.junit.ui;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

/**
 * Socket utilities.
 */
public class SocketUtil {

	private static final Random fgRandom= new Random(System.currentTimeMillis());

	/**
	 * Method that looks for an unused local port
	 * 
	 * @param searchFrom lower limit of port range
	 * @param searchTo upper limit of port range
	 */
	public static int findUnusedLocalPort(int searchFrom, int searchTo) {
		for (int i= 0; i < 10; i++) {
			int port= getRandomPort(searchFrom, searchTo);
			try {
				new Socket("127.0.0.1", port);
			} catch (SocketException e) {
				return port;
			} catch (IOException e) {
			}
		}
		return -1;
	}
	
	private static int getRandomPort(int low, int high) {
		return (int)(fgRandom.nextFloat()*(high-low))+low;
	}
}


