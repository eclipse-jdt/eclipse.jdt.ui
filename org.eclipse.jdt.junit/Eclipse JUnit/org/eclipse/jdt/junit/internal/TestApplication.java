/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.junit.internal;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.Workbench;

public class TestApplication extends Workbench {
	/**
	 * Run an event loop for the workbench.
	 */
	protected void runEventLoop() {
		// Dispatch all events.
		Display display = Display.getCurrent();
		while (true) {
			try {
				if (!display.readAndDispatch())
					break;
			} catch (Throwable e) {
				break;
			}
		}
		// Run all tests.
		String[] arguments= getCommandLineArgs();
		try {
			TestRunner.main(arguments);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		// Close the workbench.
		close();		
	}
}

