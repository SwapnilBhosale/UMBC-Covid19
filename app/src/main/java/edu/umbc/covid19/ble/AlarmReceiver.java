package edu.umbc.covid19.ble;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import edu.umbc.covid19.PrefManager;
import edu.umbc.covid19.database.DBManager;
import edu.umbc.covid19.database.DatabaseHelper;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String BROADCAST_KEY= "broadcast key";
    private static final int NUM_EPOCHS_PER_DAY = 24;
    //Length of EphID in bytes
    private static final int LENGTH_EPHID = 16;
    static List<byte[]> ephIds=null;



    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("TAG", "######### onReceive: alarm invoked");
        DBManager manager = new DBManager(context);
        PrefManager prefManager = new PrefManager(context);
        Cursor c = manager.getEphIds();

        if(c!=null && c.getCount() == 0){
            ephIds = new ArrayList<>();
            byte[] day_key = generateNewDayKey();
            Log.i("Alarm recieved", "KEY created for day : "+day_key.toString());
            getEphIDsForDay(day_key);
            Log.i("EPHID count: ", String.valueOf(ephIds.size()));
            Log.i("First EPHID: ",ephIds.get(0).toString());
            MyBleService.setEphIds(ephIds);

            String listString = ephIds.stream().map(this::bytesToHex).collect(Collectors.joining(" "));
            manager.insertEKeys(listString);
            prefManager.setDailySecretKey(bytesToHex(day_key));
            Log.i("TAG", "###### onReceive: addeds to DB "+listString+" day_key: "+day_key);
        }


    }
    public static byte[] generateNewDayKey() {
        SecureRandom random = new SecureRandom();
        byte new_key[] = new byte[32];
        random.nextBytes(new_key);
        return new_key;
    }
    //entry function
    public static List<byte[]> getEphIDsForDay(byte[] current_day_key) {

        byte[] stream_key = calculateHMAC(current_day_key);

        byte[] cipher_stream = generateCipherStream(stream_key);

        for(int i=0;i<cipher_stream.length;i+=LENGTH_EPHID) {
            byte[] ephId = Arrays.copyOfRange(cipher_stream, i, i+LENGTH_EPHID);
            ephIds.add(ephId);
        }
        //Collections.shuffle(ephIds);
        return ephIds;
    }

    public static byte[] generateNextDayKey(byte[] current_day_key) {
        return getSHAencryptedKey(current_day_key);
    }

    public static byte[] getSHAencryptedKey(byte[] day_key) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        byte[] SK= md.digest(day_key);
        return SK;
    }
    private static byte[] generateCipherStream(byte[] stream_key) {


        Cipher cipher=null;
        try {
            cipher = Cipher.getInstance("AES/CTR/NoPadding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        byte[] nonce = new byte[96 / 8];
        byte[] iv = new byte[128 / 8];
        System.arraycopy(nonce, 0, iv, 0, nonce.length);

        Key keySpec = new SecretKeySpec(stream_key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        try {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }

        byte[] allZero = new byte[LENGTH_EPHID * NUM_EPOCHS_PER_DAY];
        byte[] ciphertext = null;
        try {
            ciphertext = cipher.doFinal(allZero);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            System.out.println("Error while generating cipher stream");
            e.printStackTrace();
        }
        System.out.println(ciphertext);
        return ciphertext;
    }

    //PRF function
    static public byte[] calculateHMAC(byte[] current_day_key) {
        byte[] BK = BROADCAST_KEY.getBytes();//"FIXED_MESSAGE".getBytes();

        byte[] hmacSha256 = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(current_day_key, "HmacSHA256");
            mac.init(secretKeySpec);
            hmacSha256 = mac.doFinal(BK);
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate hmac-sha256", e);
        }
        return hmacSha256;
    }


}
