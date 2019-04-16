package it.baccan.html2pop3.exceptions;

/**
 * Class that rappresent an error while deleting the message
 *
 * @author gettingout <gettingout@linux.it>
 * @version 1.0.0
 *
 * Copyright (C) 2004 gettingout <gettingout@linux.it>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Licence details at http://www.gnu.org/licenses/gpl.txt
 */
public class DeleteMessageException extends Exception {

    /**
     */
    public DeleteMessageException() {
        super();
    }

    /**
     * @param s
     */
    public DeleteMessageException(String s) {
        super(s);
    }
}
