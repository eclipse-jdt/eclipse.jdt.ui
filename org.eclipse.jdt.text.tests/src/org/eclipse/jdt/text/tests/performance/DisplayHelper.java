/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests.performance;

import junit.framework.Assert;

import org.eclipse.swt.widgets.Display;

/**
 * Runs the event loop of the given display until {@link #condition()} becomes
 * <code>true</code> or no events have occurred for the supplied timeout. Between
 * running the event loop, {@link Display#sleep()} is called.
 *  
 * @since 3.1
 */
public class DisplayHelper {
	/**
	 * Creates a new instance.
	 */
	public DisplayHelper() {
	}
	
	/**
	 * Until <code>condition</code> becomes <code>true</code> or the timeout
	 * elapses, call <code>Display.sleep()</code> and run the event loop.
	 * 
	 * @param display the display to run the event loop of
	 * @param timeout the timeout in milliseconds
	 * @return <code>true</code> if the condition became <code>true</code>,
	 *         <code>false</code> if the timeout elapsed
	 */
	public final boolean waitForCondition(Display display, int timeout) {
		runEventQueue(display);
		DisplayWaiterThread thread= new DisplayWaiterThread(display, timeout);
		while (!condition() && !thread.hasTimedOut()) {
			thread.start();
			display.sleep();
			runEventQueue(display);
		}
		
		thread.stop();
		return !thread.hasTimedOut();
	}
	
	/**
	 * The condition - the default implementation returns <code>true</code>.
	 * Override to produce something more meaningful.
	 * 
	 * @return <code>true</code> if the condition is reached,
	 *         <code>false</code> if the event loop should be driven some more
	 */
	public boolean condition() {
		return true;
	}

	/**
	 * Runs the event loop on the given display.
	 * 
	 * @param display the display
	 */
	private void runEventQueue(Display display) {
		while (display.readAndDispatch()) {
			// do nothing
		}
	}

}

/**
 * Implements the thread that will wait for the timeout and wake up the display
 * so it does not wait forever. The thread may be restarted after it was stopped
 * or timed out.
 * 
 * @since 3.1
 */
final class DisplayWaiterThread {
	// configuration
	private final Display fDisplay;
	private final int fTimeout;
	private final Object fMutex= new Object();
	
	/* State -- possible transitions:
	 * 
	 * STOPPED -> STARTING
	 * STARTING -> RUNNING
	 * STARTING -> STOPPED
	 * RUNNING   -> STOPPED
	 * RUNNING   -> TIMED_OUT
	 * TIMED_OUT -> STARTING
	 */
	private static final class State {}
	private static final State STARTING= new State();
	private static final State RUNNING= new State();
	private static final State STOPPED= new State();
	private static final State TIMED_OUT= new State();
	
	/** The current state. */
	private State fState;
	/** The time in milliseconds (see Date) that the timeout will occur. */
	private long fNextTimeout;
	/** The thread. */
	private Thread fThread;

	/**
	 * Creates a new instance on the given display and timeout.
	 * 
	 * @param display the display to run the event loop of
	 * @param timeout the timeout to wait, must be &gt; 0
	 */
	public DisplayWaiterThread(Display display, int timeout) {
		Assert.assertNotNull(display);
		Assert.assertTrue(timeout > 0);
		fDisplay= display;
		fTimeout= timeout;
		fState= STOPPED;
	}
	
	/**
	 * Starts the timeout thread if it is not currently started. Nothing happens
	 * if a thread is already running.
	 */
	public void start() {
		synchronized (fMutex) {
			if (fState == STOPPED || fState == TIMED_OUT)
				startThread();
		}
	}
	
	/**
	 * Starts the thread if it is not currently running; resets the timeout if
	 * it is.
	 */
	public void restart() {
		synchronized (fMutex) {
			if (fState == STOPPED || fState == TIMED_OUT)
				startThread();
			else if (fState == RUNNING)
				restartTimeout();
			// else: nothing to do, already being started
		}
	}

	/**
	 * Start the thread. Assume the current state is STOPPED or TIMED_OUT.
	 */
	private void startThread() {
		allowStates(new State[] {STOPPED, TIMED_OUT});
		fState= STARTING;
		fThread= new Thread() {
			public void run() {
				synchronized (fMutex) {
					allowStates(new State[] {STARTING, STOPPED});
					if (fState == STOPPED)
						return;
					
					fState= RUNNING;
					fNextTimeout= System.currentTimeMillis() + fTimeout;
					
					try {
						long delta;
						while (fThread == this && fState == RUNNING && (delta = fNextTimeout - System.currentTimeMillis()) > 0) {
							allowStates(new State[] {RUNNING});
							fMutex.wait(delta);
						}
					} catch (InterruptedException e) {
						Assert.assertTrue("reaper thread interrupted - not possible", false);
					} finally {
						if (fThread != this)
							return; // if we were stopped and restarted, don't do anything any more
						allowStates(new State[] {RUNNING, STOPPED});
						if (fState == RUNNING) {
							fState= TIMED_OUT;
							fDisplay.wake(); // wake up call!
						}
						allowStates(new State[] {STOPPED, TIMED_OUT});
					}
				}
				
			}
		};
		fThread.start();
	}
	
	/**
	 * Resets the timeout. Assume current state is RUNNING.
	 */
	private void restartTimeout() {
		allowStates(new State[] {RUNNING});
		fNextTimeout= System.currentTimeMillis() + fTimeout;
	}

	/**
	 * Stops the thread if it is running. If not, nothing happens.
	 */
	public void stop() {
		synchronized (fMutex) {
			if (fState == RUNNING || fState == STARTING) {
				fState= STOPPED;
				fMutex.notifyAll();
			}
		}
	}

	/**
	 * Returns <code>true</code> if the thread timed out, <code>false</code> if
	 * the thread was never started, is still running, or if it was stopped by
	 * calling {@link #stop()}.
	 * 
	 * @return <code>true</code> if the thread timed out, <code>false</code> if not
	 */
	public boolean hasTimedOut() {
		synchronized (fMutex) {
			return fState == TIMED_OUT;
		}
	}
	
	/**
	 * Implements state consistency checking.
	 * 
	 * @param states the allowed states
	 * @throws junit.framework.AssertionFailedError if the current state is not
	 *         in <code>states</code>
	 */
	private final void allowStates(State[] states) {
		for (int i= 0; i < states.length; i++) {
			if (fState == states[i])
				return;
		}
		Assert.assertTrue("illegal state", false);
	}
}