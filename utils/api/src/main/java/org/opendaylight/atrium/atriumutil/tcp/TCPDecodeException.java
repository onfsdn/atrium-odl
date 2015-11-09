/*
 * This module is inserted by wipro and it is taken from 
 * https://java.net/projects/slamd/sources/svn/content/trunk/slamd/tools/LDAPDecoder/src/com/sun/snoop/SnoopException.java?rev=226
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.atrium.atriumutil.tcp;

public class TCPDecodeException extends Exception {
	// The parent exception that triggered this snoop exception.
	Exception parentException;

	/**
	 * Creates a new snoop exception with the provided message.
	 *
	 * @param message
	 *            The message explaining the reason for this exception.
	 */
	public TCPDecodeException(String message) {
		super(message);

		this.parentException = null;
	}

	/**
	 * Creates a new snoop exception with the provided message.
	 *
	 * @param message
	 *            The message explaining the reason for this exception.
	 * @param parentException
	 *            The parent exception that triggered this snoop exception.
	 */
	public TCPDecodeException(String message, Exception parentException) {
		super(message);

		this.parentException = parentException;
	}

	/**
	 * Retrieves the parent exception that triggered this snoop exception.
	 *
	 * @return The parent exception that triggered this snoop exception, or
	 *         <CODE>null</CODE> if no parent exception is available.
	 */
	public Exception getParentException() {
		return parentException;
	}
}
