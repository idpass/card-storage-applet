package org.idpass.datastorage;

import javacard.framework.JCSystem;
import javacard.security.RandomData;
import javacardx.crypto.Cipher;

import org.idpass.tools.Utils;

/**
 * Virtual Cards repository
 * 
 * @author m.samarskiy
 */
final class VirtualCardRepository {

    private static final short ACTIVATED_INDEX = 0;

    private static final short COMMON_ID       = (short) 0x8000;

    private VirtualCard        commonVirtualCard;
    private VirtualCard[]      privateVirtualCards;

    private VirtualCard        currentVirtualCard;
    private short              currentItemIndex;

    private boolean[]          activated;

    private RandomData         random;

    private Cipher             cipher;

    /**
     * Factory Method to create VirtualCardRepository instance
     * 
     * @return VirtualCardRepository instance
     */
    static VirtualCardRepository create() {
        return new VirtualCardRepository();
    }

    /**
     * Switch to the next VirtualCard
     */
    void switchNextVirtualCard() {
        if (!isActivated())
            return;

        if (currentVirtualCard == commonVirtualCard) {
            currentVirtualCard = privateVirtualCards[currentItemIndex];
        } else {
            currentVirtualCard = commonVirtualCard;
        }
    }

    /**
     * Get current active VirtualCard
     * 
     * @return current VirtualCard. null if not activated
     */
    VirtualCard getCurrentVirtualCard() {
        if (!isActivated())
            return null;
        return currentVirtualCard;
    }

    /**
     * Add new VirtualCard
     * 
     * @param newIndex
     *            new VirtualCard index
     */
    void add(short newIndex) {
        boolean foundNewItem = newIndex < privateVirtualCards.length;

        if (!foundNewItem) {
            short extendCount = (short) (newIndex - privateVirtualCards.length + 1);
            extendArray(extendCount);
        }

        VirtualCard newVirtualCard = createVirtualCard(newIndex);
        privateVirtualCards[newIndex] = newVirtualCard;

        Utils.requestObjectDeletion();
    }

    /**
     * Delete VirtualCard
     * 
     * @param index
     *            VirtualCard index
     * @return true if deleted
     */
    boolean delete(short index) {
        if (privateVirtualCards.length <= index)
            return false;

        if (privateVirtualCards[index] == null) {
            return true;
        }

        privateVirtualCards[index] = null;
        Utils.requestObjectDeletion();
        return true;
    }

    /**
     * Check if VirtualCard exists
     * 
     * @param index
     *            VirtualCard index
     * @return true if VirtualCard exists
     */
    boolean exists(short index) {
        return !(privateVirtualCards.length <= index || privateVirtualCards[index] == null);
    }

    /**
     * Activate access VirtualCard in repo
     * 
     * @param index
     *            VirtualCard index
     */
    void activate(short index) {
        activated[ACTIVATED_INDEX] = true;

        currentItemIndex = index;

        currentVirtualCard = commonVirtualCard;
    }

    /**
     * Check if repository is activated
     * 
     * @return true if activated
     */
    boolean isActivated() {
        return activated[ACTIVATED_INDEX];
    }

    private VirtualCard createVirtualCard(short id) {
        return new DesfireCard(id, random, cipher);
    }

    private void extendArray(short extendCount) {
        VirtualCard[] arr = new VirtualCard[(short) (privateVirtualCards.length + extendCount)];

        for (short i = 0; i < privateVirtualCards.length; i++) {
            arr[i] = privateVirtualCards[i];
        }

        privateVirtualCards = arr;
        Utils.requestObjectDeletion();
    }

    private VirtualCardRepository() {
        random = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        cipher = Cipher.getInstance(Cipher.ALG_DES_ECB_NOPAD, false);

        privateVirtualCards = new VirtualCard[0];
        commonVirtualCard = createVirtualCard(COMMON_ID);

        activated = JCSystem.makeTransientBooleanArray((short) (ACTIVATED_INDEX + 1), JCSystem.CLEAR_ON_RESET);
    }
}
