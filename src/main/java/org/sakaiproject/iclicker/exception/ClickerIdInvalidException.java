/**
 * Copyright (c) 2009 i>clicker (R) <http://www.iclicker.com/dnn/>
 *
 * This file is part of i>clicker Sakai integrate.
 *
 * i>clicker Sakai integrate is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * i>clicker Sakai integrate is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with i>clicker Sakai integrate.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.sakaiproject.iclicker.exception;

/**
 * Exception which indicates a failure in the clickerId, {@link #failure} indicates the {@link Failure} enum: EMPTY, LENGTH, CHARS, CHECKSUM, SAMPLE
 */
public class ClickerIdInvalidException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public enum Failure {
        /**
         * the clickerId is null or empty string
         */
        EMPTY,
        /**
         * the clickerId length is not 8 chars (too long), shorter clickerIds are padded out to 8
         */
        LENGTH,
        /**
         * the clickerId contains invalid characters
         */
        CHARS,
        /**
         * the clickerId did not validate using the checksum method
         */
        CHECKSUM,
        /**
         * the clickerId matches the sample one and cannot be used
         */
        SAMPLE,
        /**
         * the GO ID contains invalid characters
         */
        GO_CHARS,
        /**
         * cannot find and compare the current user
         */
        GO_NO_USER,
        /**
         * current user lastname does not match the one on record
         */
        GO_LASTNAME,
        /**
         * GO ID cannot be found on the server
         */
        GO_NO_MATCH,
    }

    public Failure failure;
    public String clickerId;

    public ClickerIdInvalidException(String message, Failure failure, String clickerId) {
        this(message, failure, clickerId, null);
    }

    public ClickerIdInvalidException(String message, Failure failure, String clickerId, Throwable cause) {
        super(message, cause);
        this.failure = failure;
        this.clickerId = clickerId;
    }

}
