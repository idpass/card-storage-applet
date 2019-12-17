/**
 * 
 */
package org.idpass.datastorage;

import javacard.framework.AID;
import javacard.framework.APDU;
import javacard.framework.ISO7816;
import javacard.framework.ISOException;
import javacard.framework.Shareable;
import javacard.framework.Util;

import org.idpass.tools.IdpassApplet;
import org.idpass.tools.SIOAuthListener;
import org.idpass.tools.Utils;

/**
 * Applet for personas data storage
 * 
 * @author m.samarskiy
 */
public class DatastorageApplet extends IdpassApplet implements SIOAuthListener {

    // Switch virtual card. No security
    private static final byte  INS_SWITCH_VIRTUAL_CARD = (byte) 0x9C;
    private static final byte  P1_SWITCH_VIRTUAL_CARD  = (byte) 0x00;
    private static final byte  P2_SWITCH_VIRTUAL_CARD  = (byte) 0x00;

    // default secret for SIO
    private static final byte  DEFAULT_SECRET          = (byte) 0x9E;
    private static final short NO_ACTIVE_VIRTUAL_CARDS = (short) 0xFFFF;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        byte lengthAID = bArray[bOffset];
        short offsetAID = (short) (bOffset + 1);
        short offset = bOffset;
        offset += (bArray[offset]); // skip aid
        offset++;
        offset += (bArray[offset]); // skip privileges
        offset++;

        // default params

        byte secret = DEFAULT_SECRET;

        // read params
        short lengthIn = bArray[offset];
        if (lengthIn != 0) {

            if (1 <= lengthIn) {
                // param 1 - not mandatory
                secret = bArray[(short) (offset + 1)];
            }

        }

        // GP-compliant JavaCard applet registration
        DatastorageApplet applet = new DatastorageApplet(secret);
        applet.register(bArray, offsetAID, lengthAID);
    }

    private byte                  secret;
    private VirtualCardRepository virtualCardRepository;

    public DatastorageApplet(byte secret) {
        this.secret = secret;
        virtualCardRepository = VirtualCardRepository.create();
    }

    /**
     * Shareable interface standard call from JCOP
     */
    public Shareable getShareableInterfaceObject(AID clientAID, byte parameter) {
        if (secret != parameter)
            return null;

        return (SIOAuthListener) this;
    }

    public void onPersonaAdded(short personaIndex) {
        virtualCardRepository.add(personaIndex);
    }

    public void onPersonaDeleted(short personaIndex) {
        virtualCardRepository.delete(personaIndex);
    }

    public void onPersonaAuthenticated(short personaIndex, short score) {
        if (!virtualCardRepository.exists(personaIndex)) {
            virtualCardRepository.add(personaIndex);
        }
        virtualCardRepository.activate(personaIndex);
    }

    protected void processSelect() {
        if (!selectingApplet()) {
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
        }

        setIncomingAndReceiveUnwrap();

        byte[] buffer = getApduData();

        VirtualCard virtualCard = virtualCardRepository.getCurrentVirtualCard();

        short length =
                       Util.setShort(buffer, Utils.SHORT_00, virtualCard == null ? NO_ACTIVE_VIRTUAL_CARDS
                                                                                : virtualCard.getId());
        setOutgoingAndSendWrap(buffer, Utils.SHORT_00, length);
    }

    protected void processInternal(APDU apdu) throws ISOException {
        switch (this.ins) {
            case INS_SWITCH_VIRTUAL_CARD:
                checkClaIsInterindustry();
                processSwitchNextVirtualCard();
                break;
            default:
                VirtualCard virtualCard = virtualCardRepository.getCurrentVirtualCard();

                if (virtualCard == null) {
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
                }

                virtualCard.process(apdu);
        }
    }

    private void processSwitchNextVirtualCard() {
        if (p1 != P1_SWITCH_VIRTUAL_CARD || p2 != P2_SWITCH_VIRTUAL_CARD) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2);
        }

        short lc = setIncomingAndReceiveUnwrap();

        if (lc != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        byte[] buffer = getApduData();

        virtualCardRepository.switchNextVirtualCard();

        VirtualCard virtualCard = virtualCardRepository.getCurrentVirtualCard();

        short length =
                       Util.setShort(buffer, Utils.SHORT_00, virtualCard == null ? NO_ACTIVE_VIRTUAL_CARDS
                                                                                : virtualCard.getId());
        setOutgoingAndSendWrap(buffer, Utils.SHORT_00, length);
    }
}