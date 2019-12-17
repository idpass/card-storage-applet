/**
 * 
 */
package org.idpass.datastorage;

import javacard.framework.APDU;
import javacard.framework.ISOException;

/**
 * VirtualCard Interface
 * 
 * @author m.samarskiy
 */
interface VirtualCard {

    /**
     * Process APDU for Virtual Card as for regular applet
     * 
     * @param apdu
     * @throws ISOException
     */
    public void process(APDU apdu) throws ISOException;

    /**
     * Get VirtualCard id
     * 
     * @return VirtualCard id
     */
    public short getId();
}
