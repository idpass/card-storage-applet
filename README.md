# Storage Applet

### General SW List

SW | DESCRIPTION
-- | -- 
0x9000 | No error
0x6982 | SCP Security Level is too low
0x6B00 | Incorrect parameters (P1,P2)
0x6700 | Wrong DATA length

### datastorage package
**datastorage** package contains applet for personas data storage. 
<br>This [Link](https://github.com/SakaZulu/java-card-desfire-emulation/tree/master/java-card-desfire-emulation/Desfire/src/des) used for VirtualCard implementation for data storage functionality - let’s call it Virtual Data Storage Card (VDSC) (see VirtualCard interface)
<br>DataStorage Applet instance stores VirtualCards for all personas, registered in Auth Applet instance.
<br>Each persona has their own Private VirtualCard, accessible ONLY after this persona authenticated in Auth Applet instance. 
<br>All personas registered in Auth Applet instance has Common VirtualCard (for such common services like Household).
<br>After a persona authenticated in Auth applet instance - common VDSC must be activated.
<br>Off-card entity (POS) is able to switch between Common and Private personas VDSC during authenticated session via SWITCH VIRTUAL CARD APDU command.

AID | DESCRIPTION
-- | --
F769647061737303 | Package AID
F769647061737303010001 | Applet AID. Last 4 digits of the AID (*0001*) is the applet version   

#### Install Parameters
ORDER | LENGTH | DESCRIPTION
-- | -- | --
0 | 1 | Secret. <br>Parameter for Shareble Interface Objects authentication. <br><br>*0x9E* - default value

If insall parameters are not set, default values will be used (*0x9E*)

#### APDU Commands

##### SELECT

Secure Channel Protocol minimum level: *no auth*

C-APDU:

DATA TYPE | LENGTH | VALUE
-- | -- | --
CLA | 1 | 0x00
INS | 1 | 0xA4
P1 | 1 | 0x04
P2 | 1 | 0x00
LC | 1 | Applet instance AID length
DATA | var | Applet instance AID

R-APDU:

DATA TYPE | LENGTH | VALUE
-- | -- | --
DATA | 2 | Current active virtual card id (equals to Index of authenticated persona)<br>If no active virtual card (no authenticated Persona) - *0xFFFF* returns
SW | 2 | Status Word (see **General SW List** section)

##### SWITCH VIRTUAL CARD
Switch virtual cards (berween common and private) for authenticated persona

Secure Channel Protocol minimum level: *no auth*

C-APDU:

DATA TYPE | LENGTH | VALUE
-- | -- | --
CLA | 1 | 0x00
INS | 1 | 0x9C
P1 | 1 | 0x00
P2 | 1 | 0x00
LC | 1 | 0x00
DATA | 0 | No input data

R-APDU:

DATA TYPE | LENGTH | VALUE
-- | -- | --
DATA | 2 | Current active virtual card id (equals to Index of authenticated persona)<br>*0xFFFF* - if no active virtual card (no authenticated Persona) returns <br>*0x8000* - if common virtual card activated
SW | 2 | Status Word (see **General SW List** section)

##### Virtual Card related APDU commands
When any Virtual Card activated, off-card entity can communicate with it via Virtual Card related APDU commands 
<br>See [MIFARE DESFire EV1 Implementation for details](https://github.com/SakaZulu/java-card-desfire-emulation/tree/master/java-card-desfire-emulation/Desfire/src/des)

### Contributors

Contributions are welcome!

- Newlogic Impact Lab
- Maksim Samarskiy
