package helpers.java;

import java.math.BigInteger;
import java.security.MessageDigest;

public class TorqueFreeUtil {

	public static String encode(String string) {
		try {
			byte[] value = MessageDigest.getInstance((String) ("MD5")).digest(
					string.getBytes());
			BigInteger string1 = new BigInteger(1, value);
			String string2 = string1.toString(16);
			while (string2.length() < 32) {
				string2 = ("0" + string2);
			}
			return string2;
		} catch (Throwable var1_3) {
			return "";
		}
	}

}
