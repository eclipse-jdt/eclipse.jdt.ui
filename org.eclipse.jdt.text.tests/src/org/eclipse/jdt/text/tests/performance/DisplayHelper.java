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
public abstract class DisplayHelper {
	/**
	 * Creates a new instance.
	 */
	protected DisplayHelper() {
	}
	
	/**
	 * Until {@link #condition()} becomes <code>true</code> or the timeout
	 * elapses, call {@link Display#sleep()} and run the event loop.
	 * <p>
	 * If <code>timeout &lt; 0</code>, the event loop is never driven and
	 * only the condition is checked. If <code>timeout == 0</code>, the event
	 * loop is driven at most once, but <code>Display.sleep()</code> is never
	 * invoked.
	 * </p>
	 * 
	 * @param display the display to run the event loop of
	 * @param timeout the timeout in milliseconds
	 * @return <code>true</code> if the condition became <code>true</code>,
	 *         <code>false</code> if the timeout elapsed
	 */
	public final boolean waitForCondition(Display display, long timeout) {
		// if the condition already holds, succeed
		if (condition())
			return true;
		
		if (timeout < 0)
			return false;
		
		// if driving the event loop once makes the condition hold, succeed
		// without spawning a thread.
		runEventQueue(display);
		if (condition())
			return true;
		
		// if the timeout is negative or zero, fail
		if (timeout == 0)
			return false;

		// repeatedly sleep until condition becomes true or timeout elapses
		DisplayWaiter waiter= new DisplayWaiter(display, timeout);
		DisplayWaiter.Timeout timeoutState= waiter.start();
		boolean condition;
		do {
			if (display.sleep())
				runEventQueue(display);
			condition= condition();
		} while (!condition && !timeoutState.hasTimedOut());
		
		waiter.stop();
		return condition;
	}
	
	/**
	 * Call {@link Display#sleep()} and run the event loop until the given
	 * timeout has elapsed.
	 * <p>
	 * If <code>timeout &lt; 0</code>, nothing happens. If
	 * <code>timeout == 0</code>, the event loop is driven exactly once, but
	 * <code>Display.sleep()</code> is never invoked.
	 * </p>
	 * 
	 * @param display the display to run the event loop of
	 * @param millis the timeout in milliseconds
	 */
	public static void sleep(Display display, long millis) {
		new DisplayHelper() {
			public boolean condition() {
				return false;
			}
		}.waitForCondition(display, millis);
	}
	
	/**
	 * The condition which causes {@link #waitForCondition(Display, int)} to
	 * wait for the entire timeout.
	 * 
	 * @return <code>true</code> if the condition is reached,
	 *         <code>false</code> if the event loop should be driven some more
	 */
	public abstract boolean condition();

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
final class DisplayWaiter {
	/**
	 * Timeout state of a display waiter thread.
	 */
	public final class Timeout {
		private boolean fTimeoutState= false;
		/**
		 * Returns <code>true</code> if the timeout has been reached,
		 * <code>false</code> if not.
		 * 
		 * @return <code>true</code> if the timeout has been reached,
		 *         <code>false</code> if not
		 */
		public boolean hasTimedOut() {
			synchronized (fMutex) {
				return fTimeoutState;
			}
		}
		void setTimedOut(boolean timedOut) {
			fTimeoutState= timedOut;
		}
		Timeout(boolean initialState) {
			fTimeoutState= initialState;
		}
	}
	
	// configuration
	private final Display fDisplay;
	private final long fTimeout;
	private final Object fMutex= new Object();
	private final boolean fKeepRunningOnTimeout;
	
	/* State -- possible transitions:
	 * 
	 * STOPPED   -> RUNNING
	 * RUNNING   -> STOPPED
	 * RUNNING   -> IDLE
	 * IDLE      -> RUNNING
	 * IDLE      -> STOPPED
	 */
	private static final int RUNNING= 1 << 1;
	private static final int STOPPED= 1 << 2;
	private static final int IDLE= 1 << 3;
	
	/** The current state. */
	private int fState;
	/** The time in milliseconds (see Date) that the timeout will occur. */
	private long fNextTimeout;
	/** The thread. */
	private Thread fCurrentThread;
	/** The timeout state of the current thread. */
	private Timeout fCurrentTimeoutState;

	/**
	 * Creates a new instance on the given display and timeout.
	 * 
	 * @param display the display to run the event loop of
	 * @param timeout the timeout to wait, must be &gt; 0
	 */
	public DisplayWaiter(Display display, long timeout) {
		this(display, timeout, false);
	}
	
	/**
	 * Creates a new instance on the given display and timeout.
	 * 
	 * @param display the display to run the event loop of
	 * @param timeout the timeout to wait, must be &gt; 0
	 * @param keepRunning <code>true</code> if the thread should be kept running after timing out
	 */
	public DisplayWaiter(Display display, long timeout, boolean keepRunning) {
		Assert.assertNotNull(display);
		Assert.assertTrue(timeout > 0);
		fDisplay= display;
		fTimeout= timeout;
		fState= STOPPED;
		fKeepRunningOnTimeout= keepRunning;
	}
	
	/**
	 * Starts the timeout thread if it is not currently running. Nothing happens
	 * if a thread is already running.
	 * 
	 * @return the timeout state which can be queried for its timed out status
	 */
	public Timeout start() {
		synchronized (fMutex) {
			switch (fState) {
				case STOPPED:
					startThread();
					break;
				case IDLE:
					unhold();
					break;
				case RUNNING:
					break;
			}
			
			return fCurrentTimeoutState;
		}
	}
	
	/**
	 * Starts the thread if it is not currently running; resets the timeout if
	 * it is.
	 * 
	 * @return the timeout state which can be queried for its timed out status
	 */
	public Timeout restart() {
		synchronized (fMutex) {
			switch (fState) {
				case STOPPED:
					startThread();
					break;
				case IDLE:
					unhold();
					break;
				case RUNNING:
					restartTimeout();
					break;
			}
			
			return fCurrentTimeoutState;
		}
	}

	/**
	 * Stops the thread if it is running. If not, nothing happens. Another
	 * thread may be started by calling {@link #start()} or {@link #restart()}.
	 */
	public void stop() {
		synchronized (fMutex) {
			if (tryTransition(RUNNING | IDLE, STOPPED))
				fMutex.notifyAll();
		}
	}
	
	/**
	 * Puts the reaper thread on hold but does not stop it. It may be restarted
	 * by calling {@link #start()} or {@link #restart()}.
	 */
	public void hold() {
		synchronized (fMutex) {
			// nothing to do if there is no thread
			if (tryTransition(RUNNING, IDLE))
				fMutex.notifyAll();
		}
	}

	/**
	 * Resets the timeout. Assume current state is RUNNING.
	 */
	private void restartTimeout() {
		assertStates(RUNNING);
		fNextTimeout= System.currentTimeMillis() + fTimeout;
	}

	/**
	 * Transition to RUNNING and clear the timed out flag. Assume current state
	 * is IDLE.
	 */
	private void unhold() {
		checkedTransition(IDLE, RUNNING);
		fCurrentTimeoutState= new Timeout(false);
		fMutex.notifyAll();
	}
		
	/**
	 * Start the thread. Assume the current state is STOPPED.
	 */
	private void startThread() {
		checkedTransition(STOPPED, RUNNING);
		fCurrentTimeoutState= new Timeout(false);
		fCurrentThread= new Thread() {
			/**
			 * Exception thrown when a thread notices that it has been stopped
			 * and a new thread has been started.
			 */
			final class ThreadChangedException extends Exception {
				private static final long serialVersionUID= 1L;
			}

			/*
			 * @see java.lang.Runnable#run()
			 */
			public void run() {
				try {
					run2();
				} catch (InterruptedException e) {
					// ignore and end the thread - we never interrupt ourselves,
					// so it must be an external entity that interrupted us
				} catch (ThreadChangedException e) {
					// the thread was stopped and restarted before we got out
					// of a wait - we're no longer used
					// we might have been notified instead of the current thread,
					// so wake it up
					synchronized (fMutex) {
						fMutex.notifyAll();
					}
				}
			}
			
			private void run2() throws InterruptedException, ThreadChangedException {
				synchronized (fMutex) {
					checkThread();
					tryHold(); // potential state change
					assertStates(STOPPED | RUNNING);
					
					while (isState(RUNNING)) {
						waitForTimeout(); // potential state change
						
						if (isState(RUNNING))
							timedOut(); // state change
						assertStates(STOPPED | IDLE);
						
						tryHold(); // potential state change
						assertStates(STOPPED | RUNNING);
					}
					assertStates(STOPPED);
				}
			}

			/**
			 * Check whether the current thread is this thread, throw an
			 * exception otherwise.
			 * 
			 * @throws ThreadChangedException if the current thread changed
			 */
			private void checkThread() throws ThreadChangedException {
				if (fCurrentThread != this)
					throw new ThreadChangedException();
			}

			/**
			 * Waits until the next timeout occurs.
			 * 
			 * @throws InterruptedException if the thread was interrupted
			 * @throws ThreadChangedException if the thread changed
			 */
			private void waitForTimeout() throws InterruptedException, ThreadChangedException {
				fNextTimeout= System.currentTimeMillis() + fTimeout;
				
				long delta;
				while (isState(RUNNING) && (delta = fNextTimeout - System.currentTimeMillis()) > 0) {
					fMutex.wait(delta);
					checkThread();
				}
			}

			/**
			 * Sets the timed out flag and wakes up the display. Transitions
			 * to IDLE (if in keep-running mode) or STOPPED.
			 */
			private void timedOut() {
				fCurrentTimeoutState.setTimedOut(true);
				fDisplay.wake(); // wake up call!
				if (fKeepRunningOnTimeout)
					checkedTransition(RUNNING, IDLE);
				else
					checkedTransition(RUNNING, STOPPED);
			}
			
			/**
			 * Waits while the state is IDLE, then returns. The state must not
			 * be RUNNING when calling this method. The state is either STOPPED
			 * or RUNNING when the method returns.
			 * 
			 * @throws InterruptedException if the thread was interrupted
			 * @throws ThreadChangedException if the thread has changed while on
			 *         hold
			 */
			private void tryHold() throws InterruptedException, ThreadChangedException {
				while (isState(IDLE)) {
					fMutex.wait(0);
					checkThread();
				}
				assertStates(STOPPED | RUNNING);
			}
		};
		
		fCurrentThread.start();
	}
	
	/**
	 * Transitions to nextState if the current state is one of possibleStates.
	 * Returns <code>true</code> if the transition happened,
	 * <code>false</code> otherwise.
	 * 
	 * @param possibleStates the states which trigger a transition
	 * @param nextState the state to transition to
	 * @return <code>true</code> if the transition happened,
	 *         <code>false</code> otherwise
	 */
	private boolean tryTransition(int possibleStates, int nextState) {
		if (isState(possibleStates)) {
			fState= nextState;
			return true;
		}
		return false;
	}
	
	/**
	 * Checks the possible states and throws an assertion if it is not met, then
	 * transitions to nextState
	 * 
	 * @param possibleStates the allowed states
	 * @param nextState the state to transition to
	 */
	private void checkedTransition(int possibleStates, int nextState) {
		assertStates(possibleStates);
		fState= nextState;
	}
	
	/**
	 * Implements state consistency checking.
	 * 
	 * @param states the allowed states
	 * @throws junit.framework.AssertionFailedError if the current state is not
	 *         in <code>states</code>
	 */
	private void assertStates(int states) {
		Assert.assertTrue("illegal state", isState(states));
	}

	/**
	 * Answers <code>true</code> if the current state is in the given states.
	 * 
	 * @param states the possible states
	 * @return <code>true</code> if the current state is in the given states,
	 *         <code>false</code> otherwise
	 */
	private boolean isState(int states) {
		return (states & fState) == fState;
	}
}