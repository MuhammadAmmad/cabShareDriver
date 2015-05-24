package in.co.hoi.cabshare;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class Encryptor{

    private static final String ALGORITHM = "AES";

    private static final int ITERATIONS = 2;

    private static final byte[] keyValue = new byte[] { 'A', 'p', 'I', '#', '-', 'H', 'O', 'I', '5', '-', 'A', 'P', 'S', '8', '@', '7'};

    //private static final char[] keyValue = new char[] { 'A', 'p', 'I', '3', '-', 'H', 'O', 'I', '9', '-', 'A', 'P', 'S', '8', '@', '7'};


    public String getEncryptedText (String text, String key) throws InvalidKeyException,

            IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, DecoderException {

        Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = null;

        try {

            cipher = Cipher.getInstance("AES");

        } catch (NoSuchAlgorithmException e) {

            // TODO Auto-generated catch block

            e.printStackTrace();

        } catch (NoSuchPaddingException e) {

            // TODO Auto-generated catch block

            e.printStackTrace();

        }

        cipher.init(Cipher.ENCRYPT_MODE, aesKey);

        byte[] b = new byte[0];
        try {
            b = Hex.decodeHex(text.toCharArray());
        } catch (DecoderException e) {
            //Todo catch block for decoding hex
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        byte[] encrypted = cipher.doFinal(b);

        char[] str = Hex.encodeHex(encrypted);

        return new String(str);

    }

    public String getEncryptedKeyValue (String key) throws InvalidKeyException,

            IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException, DecoderException {

        Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
        Cipher cipher = null;

        try {

            cipher = Cipher.getInstance("AES");

        } catch (NoSuchAlgorithmException e) {

            // TODO Auto-generated catch block

            e.printStackTrace();

        } catch (NoSuchPaddingException e) {

            // TODO Auto-generated catch block

            e.printStackTrace();

        }

        cipher.init(Cipher.ENCRYPT_MODE, aesKey);


        byte[] encrypted = cipher.doFinal(keyValue);

        char[] str = Hex.encodeHex(encrypted);

        System.out.println(new String(str));

        return new String(str);
    }
}


