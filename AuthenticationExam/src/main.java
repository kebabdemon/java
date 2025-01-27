import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;

/*
01,SaltA:HashA;
02,SaltB:HashB
*/


class Main {
public static void main(String args[]) throws NoSuchAlgorithmException, IOException {  //static method{
        HashMap<String, String> map = new HashMap<String, String>();
        BufferedReader br = new BufferedReader(new FileReader("data.csv"));
        String line =  null;

        while((line=br.readLine())!=null) {
                String[] str = line.split(";");

                for (int i = 0; i < str.length; i++) {
                        String arr[] = str[i].split(":");
                        System.out.println(str[i]);

                       String userID = arr[0].split(",")[0];
                        String salt = arr[0].split(",")[1];
                        String hash = arr[1];


                        System.out.println("BELOW IS FINAL-------");
                        System.out.println(userID);
                        System.out.println(salt);
                        System.out.println(hash);
                        System.out.println("--------------");

                        map.put(userID, "" + salt + ":" + hash);
                }
        }

        System.out.print(map);


        // NAMES and CLEAR Text Passwords
        // user_id;nejaky_nahodny_retezec_coby_salt;hashovane_osolene_heslo




        class ToCSV {
                final String name;
                final String clearText;

                ToCSV(String name, String clearText){
                        this.name = name;
                        this.clearText = clearText;
                }
        }

        ToCSV[] scenes = {
                new ToCSV("UserA", "passwordA"),
                new ToCSV("UserB", "passwordB"),
                new ToCSV("UserC", "passwordC")
        };

        FileWriter writer = new FileWriter("passwordsFinal.csv");
        for (int i = 0; i < scenes.length; i++){
                String name = scenes[i].name;
                String clearText = scenes[i].clearText;

                SecureRandom random = new SecureRandom();
                byte[] salt = new byte[16];
                random.nextBytes(salt);

                MessageDigest md = MessageDigest.getInstance("SHA-512");
                md.update(salt);

                byte[] hashedPassword = md.digest(clearText.getBytes(StandardCharsets.UTF_8));


                String encodedPassword = Base64.getEncoder().encodeToString(hashedPassword);
                System.out.println("BASE64: " + encodedPassword);

                String saltToString = new String(salt, StandardCharsets.UTF_8);

                String encodedSalt = Base64.getEncoder().encodeToString(salt);
                String hashedPasswordString = encodedPassword;//new String(/*hashedPassword*/encodedPassword);


                if (hashedPasswordString.equals("�n\u0010=��`\u0013ɇ���\\#�MN�iXί��ʅ�[�3�Z<\u0012�\u001CB�.��R�}\u0004���\u0002���")){
                        System.out.println("SOMETHING MATCHES!");
                }

                System.out.println("ID: " + name + " \n SALT: " + saltToString);
                System.out.println("\n hashedSalt:" + hashedPasswordString + "\n CLEARTEXT: " + clearText);


                // TO CSV: ID, SALT, HASHEDPASSWORDSTRING
                // CREATE CSV File
                writer.write(name + ";" + encodedSalt + ";" + hashedPasswordString + "," + "\n");


        }

        writer.close();

        // COMPARE WITH PASSWORDS:
        // VÝSTUP MÉHO SERVERU:
        // UserA;�*����[�V��z;^��E�\�*{]�m��ʠ^ۻh��ɽ8�VG<�Bv�s���u���y�6�� Aǉ��







        /*
        *    // 1 - SALT THE PASSWORd
        System.out.println("Static method");

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);


        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt);

        byte[] hashedPassword = md.digest("Filip".getBytes(StandardCharsets.UTF_8));
        System.out.println(hashedPassword);
        * */





























        }
}

