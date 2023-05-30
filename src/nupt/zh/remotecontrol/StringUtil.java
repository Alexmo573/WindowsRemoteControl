package nupt.zh.remotecontrol;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 工具类
 *
 * @author zh
 *
 */
public class StringUtil {

	public static boolean isPortUsed(int port) {
		try {
			DatagramSocket socket = new DatagramSocket(port);
			socket.close();
			return true;
		} catch (SocketException e) {
			return false;
		}
	}

	public static boolean isValidIP(String ipAddress) {
		String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
		Pattern pattern = Pattern.compile(ip);
		Matcher matcher = pattern.matcher(ipAddress);
		return matcher.matches();
	}

	public static boolean isValidPort(String port) {
		try {
			int temp = Integer.parseInt(port);
			if (temp > 0 && temp < 65535) {
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public static boolean isValidPath(String path) {
		String regex = "^(?<path>(?:[a-zA-Z]:)?\\\\(?:[^\\\\\\?\\/\\*\\|<>:\"]+\\\\)+)(?<filename>(?<name>[^\\\\\\?\\/\\*\\|<>:\"]+?)\\.(?<ext>[^.\\\\\\?\\/\\*\\|<>:\"]+))$";
		Pattern pattern=Pattern.compile(regex);
		Matcher matcher=pattern.matcher(path);
		return matcher.matches();

	}
}
