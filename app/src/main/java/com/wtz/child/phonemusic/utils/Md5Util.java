package com.wtz.child.phonemusic.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Md5Util {
	/**
	 * 根据文件计算出文件的MD5
	 * 
	 * @param file
	 * @return
	 */
	public static String getFileMD5(File file) {
		if (!file.isFile()) {
			return null;
		}

		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			in.close();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// BigInteger bigInt = new BigInteger(1, digest.digest());
		// return bigInt.toString(16);
		// 采用上述方式会把首位为0去掉
		return byteArrayToHex(digest.digest());
	}

	/**
	 * 获取文件夹中的文件的MD5值
	 * 
	 * @param file
	 * @param listChild
	 * @return
	 */
	public static Map<String, String> getDirMD5(File file, boolean listChild) {
		if (!file.isDirectory()) {
			return null;
		}

		Map<String, String> map = new HashMap<String, String>();
		String md5;

		File[] files = file.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file2 = files[i];
			if (file2.isDirectory() && listChild) {
				map.putAll(getDirMD5(file2, listChild));
			} else {
				md5 = getFileMD5(file2);
				if (md5 != null) {
					map.put(file2.getPath(), md5);
				}
			}
		}
		return map;
	}

	public static String getBytesMD5(byte[] content) {
		if (content == null) {
			return "";
		}

		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			return byteArrayToHex(digest.digest(content));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	public static String getStringMD5(String content) {
		if (content == null) {
			return "";
		}

		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
			return byteArrayToHex(digest.digest(content.getBytes()));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
	}

	private static String byteArrayToHex(byte[] byteArray) {
		// 首先初始化一个字符数组，用来存放每个16进制字符
		char[] hexDigits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

		// new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
		char[] resultCharArray = new char[byteArray.length * 2];

		// 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
		int index = 0;
		for (byte b : byteArray) {
			resultCharArray[index++] = hexDigits[b >>> 4 & 0xf];
			resultCharArray[index++] = hexDigits[b & 0xf];
		}

		return new String(resultCharArray);
	}
}
