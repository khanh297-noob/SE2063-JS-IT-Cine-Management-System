/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author adm
 */
public class PasswordUtil {

    /**
     * method kiểm tra password nhập vào có trùng với password trong database ko
     */
    public static boolean verify(String passwordPlain, String passwordHash) {
        if (passwordPlain == null || passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(passwordPlain, passwordHash);
    }

    /**
     * method sử dụng hàm băm để chuyển thành hashcode
     *
     * @param passwordPlain: password nhập vào để đăng kí
     * @return
     */
    public static String hash(String passwordPlain) {
        return BCrypt.hashpw(passwordPlain, BCrypt.gensalt(12));
    }

//    public static void main(String[] args) {
//        String hash = BCrypt.hashpw("123456", BCrypt.gensalt(10));
//        System.out.println(hash);
//    }
}
