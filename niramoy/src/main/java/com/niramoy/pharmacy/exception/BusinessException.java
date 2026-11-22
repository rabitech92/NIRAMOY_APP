package com.niramoy.pharmacy.exception;

/** ব্যবসায়িক নিয়ম ভাঙলে ছোঁড়া হয়; বার্তাটি সরাসরি ব্যবহারকারীকে দেখানো যায়। */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) { super(message); }
}
