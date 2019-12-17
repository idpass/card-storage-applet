package org.idpass.datastorage;

import javacard.framework.ISOException;
import javacard.security.DESKey;
import javacard.security.Key;
import javacard.security.KeyBuilder;

//El Directory File equivale a la aplicación. 

public class DirectoryFile extends File {

    //    private byte[]            AID; // not used??
    private static final byte MAX_FILES             = 32;
    public boolean[]          activatedFiles        = new boolean[32];
    private boolean[]         waitingForTransaction = new boolean[32];
    private File[]            arrayFiles            = new File[MAX_FILES];
    private Key[]             keyList;
    private byte              numberFiles           = 0;
    private Key               masterKey;
    private byte              keyType;

    //Key Settings
    private byte              changeKeyAccessRights;                      //que clave es precisa para cambiar una clave (nivel App)
    public boolean            configurationChangeable;                    //true-es posible cambiar estas settings(mkAuth requerida)
    private boolean           masterNotNeededForManage;                   //Para crear/eliminar (1-No hace falta Ath)
    private boolean           masterNotNeededForCheck;                    //Para commandos get (1-No hace falta Ath)
    private boolean           masterChangeable;                           //0-MK inmovil 1-MK cambiable (Es precisa la Master Key correspondiente)
    private byte              maxKeyNumber;                               //Maximo numero de claves que se pueden almacenar (aplicación)
    private boolean           ISOFileIDSupported;

    /**
     * Constructor for the Master File
     * 
     * @param fid
     */
    protected DirectoryFile(byte fid) {
        super(fid);//llama al constructor de la clase File
        for (byte i = 0; i < activatedFiles.length; i++) {
            activatedFiles[i] = false;
        }
        configurationChangeable = true;
        masterNotNeededForManage = true;
        masterNotNeededForCheck = true;
        masterChangeable = true;
        //La master key puede ser 3DES(16), TKDES(24) o AES(16)
        keyType = Util.TDES;
        DESKey newKey = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES3_2KEY, false);
        newKey.clearKey();
        newKey.setKey(Util.DEFAULT_MASTER_KEY, (byte) 0);
        masterKey = newKey;
        maxKeyNumber = -1;

    }

    /**
     * Constructor for the applications
     */
    protected DirectoryFile(byte fid, byte[] keySettings, DirectoryFile parent) {
        super(fid, parent);//llama al constructor de la clase File
        changeKeySettings(keySettings[0]);
        keyType = (byte) (keySettings[1] >> 6);
        maxKeyNumber = (byte) (keySettings[1] & (byte) 0x0F);
        if ((keySettings[1] & (byte) 0x10) == (byte) 0x10)
            ISOFileIDSupported = true;
        else
            ISOFileIDSupported = false;

        for (byte i = 0; i < activatedFiles.length; i++) {
            activatedFiles[i] = false;
        }
        for (byte i = 0; i < waitingForTransaction.length; i++) {
            waitingForTransaction[i] = false;
        }

        //		Key claveAux=KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES, false);

        keyList = new Key[maxKeyNumber];
        keyType = Util.TDES;
        DESKey newKey = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES3_2KEY, false);
        newKey.clearKey();
        newKey.setKey(((MasterFile) getParent()).getDefaultKey(), (byte) 0);
        keyList[0] = newKey;//Application Master Key
    }

    // not used??
    //    public void setAID(byte[] AID) {
    //    }

    public boolean isISOFileIDSupported() {
        return ISOFileIDSupported;
    }

    public byte getNumberFiles() {
        return numberFiles;
    }

    public File getFile(byte fid) {
        if (activatedFiles[fid] == true)
            return (arrayFiles[fid]);
        else {
            ISOException.throwIt((short) Util.FILE_NOT_FOUND);//File not found
            return null;
        }
    }

    /**
     * Checks if the file with the given file number exists
     * 
     * @return True if it is activated already
     */
    public boolean isValidFileNumber(byte fileN) {
        return activatedFiles[fileN];
    }

    public void updateFile(File update, byte fileID) {
        arrayFiles[fileID] = update;
    }

    public void addFile(File s) {
        if (activatedFiles[s.getFileID()] == true) {
            ISOException.throwIt(Util.DUPLICATE_ERROR);//Duplicate File
        }
        arrayFiles[s.getFileID()] = s;
        numberFiles++;
        activatedFiles[s.getFileID()] = true;

    }

    public void deleteFile(byte id) {

        activatedFiles[id] = false;
        arrayFiles[id] = null;
        numberFiles--;
    }

    public Key getKey(byte keyNumber) {
        if (keyNumber >= maxKeyNumber)
            ISOException.throwIt(Util.NO_SUCH_KEY);//No Such Key
        else if (keyList[keyNumber] == null)
            ISOException.throwIt(Util.NO_SUCH_KEY);//No Such Key
        return (keyList[keyNumber]);
    }

    public Key getMasterKey() {
        return masterKey;
    }

    public void changeKey(byte keyNumber, byte[] keyBytes) {
        if (keyNumber >= maxKeyNumber)
            ISOException.throwIt(Util.NO_SUCH_KEY);//No Such Key
        if (this.isMasterFile() == true) { //Si es Master File
            //Segun el keyNumber se decide el tipo de clave que tenemos. 
            //FALTA

            DESKey newKey = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES3_2KEY, false);
            newKey.clearKey();
            newKey.setKey(keyBytes, (byte) 0);
            masterKey = newKey;
        } else {//It's not MasterFile
            DESKey newKey = (DESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_DES, KeyBuilder.LENGTH_DES3_2KEY, false);
            newKey.clearKey();
            newKey.setKey(keyBytes, (byte) 0);
            keyList[keyNumber] = newKey;
        }
    }

    public byte getMasterKeyType() {
        return keyType;
    }

    public boolean hasChangeAccess(byte keyNAuthenticated, byte keyNToChange) {
        if (keyNToChange >= maxKeyNumber)
            ISOException.throwIt(Util.NO_SUCH_KEY);//No Such Key
        if (this.getFileID() == (byte) 0x00) {//Si es la PICC Master Key
            if ((keyNAuthenticated == (byte) 0x00) & (masterChangeable == true))
                return true;
            else
                return false;
        }
        if (changeKeyAccessRights == (byte) 0x00) {//Es necesaria mkAuth
            if (keyNAuthenticated == 0)
                return true;
            else
                return false;
        }
        if (changeKeyAccessRights == (byte) 0x0F) {//Solo se puede cambiar mk con mkAuth
            if ((keyNToChange == 0) & (keyNAuthenticated == 0) & (masterChangeable == true))
                return true;
            else
                return false;
        }
        if (changeKeyAccessRights == (byte) 0x0E) {//Es precisa la propia clave q se va a cambiar
            if ((keyNToChange == 0x00) & (masterChangeable == false))
                return false;
            if (keyNToChange == keyNAuthenticated)
                return true;
            else
                return false;
        }
        //Resto de posibilidades(0x01-0x0D):ChangeKeyAccessSettings es la propia clave necesaria
        //para cambiar cualquier clave
        if (keyNToChange == changeKeyAccessRights) {//Para cambiar la changeKey se precisa la MK
            if (keyNAuthenticated == (byte) 0x00)
                return true;
            else
                return false;
        }
        if (keyNToChange == (byte) 0x00) {//Para cambiar la MK se precisa la MK
            if (masterChangeable == false)
                return false;
            if (keyNAuthenticated == (byte) 0x00)
                return true;
            else
                return false;
        }
        if (changeKeyAccessRights == keyNAuthenticated)
            return true;//Si estamos autentificados con la changeKey
        else
            return false;
    }

    public boolean hasKeySettingsChangeAllowed(byte authenticated) {
        if (configurationChangeable == false)
            return false;
        if (authenticated == (byte) 0x00)
            return true;//Hace falta autentificacion con la master Key
        return false;
    }

    public void changeKeySettings(byte newKS) {
        if (getFileID() != (byte) 0x00) {
            changeKeyAccessRights = (byte) (((byte) (newKS >> 4)) & ((byte) 0x0F));
        }
        if ((newKS | (byte) 0xF7) == 0xF7)
            configurationChangeable = false;
        else
            configurationChangeable = true;

        if ((newKS | (byte) 0xFB) == 0xFB)
            masterNotNeededForManage = false;
        else
            masterNotNeededForManage = true;

        if ((newKS | (byte) 0xFD) == 0xFD)
            masterNotNeededForCheck = false;
        else
            masterNotNeededForCheck = true;

        if ((newKS | (byte) 0xFE) == 0xFE)
            masterChangeable = false;
        else
            masterChangeable = true;
    }

    public boolean hasGetRights(byte authenticated) {
        if (masterNotNeededForCheck)
            return true;
        else if (authenticated == (byte) 0x00)
            return true;
        return false;
    }

    public boolean hasManageRights(byte authenticated) {
        if (masterNotNeededForManage)
            return true;
        else if (authenticated == (byte) 0x00)
            return true;
        return false;
    }

    public byte getKeySettings() {
        byte ks = 0;
        if (getFileID() != (byte) 0x00) {
            ks = (byte) (changeKeyAccessRights << 4);
        }
        if (configurationChangeable == true)
            ks = (byte) (ks | (byte) 0x08);
        if (masterNotNeededForManage == true)
            ks = (byte) (ks | (byte) 0x04);
        if (masterNotNeededForCheck == true)
            ks = (byte) (ks | (byte) 0x02);
        if (masterChangeable == true)
            ks = (byte) (ks | (byte) 0x01);
        return ks;
    }

    public byte getKeyNumber() {
        byte kn = 0;
        if (getFileID() == (byte) 0x00)
            return (byte) 0x01;
        kn = (byte) (keyType << 6);
        kn = (byte) (kn | maxKeyNumber);
        return kn;
    }

    /**
     * @return True if this DF is the Master File
     */
    public boolean isMasterFile() {
        return false;
    }

    /**
     * Checks if the key exists or not
     */
    public boolean isValidKeyNumber(byte keyNumber) {

        if (keyNumber >= maxKeyNumber)
            return false;//No Such Key
        else if (keyList[keyNumber] == null)
            return false;//No Such Key
        return true;
    }

    public void setWaitForTransaction(byte fileNumber) {
        waitingForTransaction[fileNumber] = true;
    }

    public void resetWaitForTransaction(byte fileNumber) {
        waitingForTransaction[fileNumber] = false;
    }

    public boolean getWaitingForTransaction(byte fileNumber) {
        return waitingForTransaction[fileNumber];
    }
}
