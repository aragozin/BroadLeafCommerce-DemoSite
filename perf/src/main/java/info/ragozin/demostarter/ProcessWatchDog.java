package info.ragozin.demostarter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.UUID;

public class ProcessWatchDog extends Thread {

	public static boolean check(File lifeGrant) {
		try {
			Grant g = open(lifeGrant);
			if (g == null) {
				return false;
			}
			Socket sock = new Socket(InetAddress.getByAddress(new byte[] {127, 0, 0, 42}), g.port);
			if (sock.isConnected()) {
				sock.close();
				return true;
			}
			else {
				sock.close();
				return false;
			}
		}
		catch(IOException e) {
			return false;
		}
	}

	public static void kill(File lifeGrant) {
		try {
			Grant g = open(lifeGrant);
			if (g != null) {
				Socket sock = new Socket(InetAddress.getByAddress(new byte[] {127, 0, 0, 42}), g.port);
				if (sock.isConnected()) {
					sock.getOutputStream().write(g.magic.getBytes());
					sock.setSoTimeout(10 * 60 * 1000);
					sock.getInputStream().read(); // socket should be closed by process shutdown
				}
				else {
					sock.close();
				}
			}
		}
		catch(IOException e) {
		}
	}

	private static Grant open(File lifeGrant) throws IOException {
		if (lifeGrant.isFile()) {
			byte[] data = new byte[4 << 10];
			FileInputStream fis = new FileInputStream(lifeGrant);
			int n = fis.read(data);
			fis.close();
			String token = new String(data, 0, n);

			Grant g = new Grant();
			int ch = token.indexOf('\n');
			g.vmname = ch < 0 ? token : token.substring(0, ch);
			if (ch > 0) {
				int sp = token.lastIndexOf(' ');
				g.port = Integer.valueOf(token.substring(ch + 1, sp));
				g.magic = token.substring(sp + 1);
			}
			return g;
		}
		else {
			return null;
		}
	}

	private static class Grant {
		@SuppressWarnings("unused")
		String vmname;
		int port;
		String magic;
	}

	private final File lifeGrant;
	private final String vmname;
	private final ServerSocket socket;
	private final String magic = UUID.randomUUID().toString();

	public ProcessWatchDog(File lifeGrant) {
		try {
			if (lifeGrant.getParentFile() != null) {
				lifeGrant.getParentFile().mkdirs();
			}
			setDaemon(true);
			setName("ProcessWatchDog");
			vmname = ManagementFactory.getRuntimeMXBean().getName();
			this.lifeGrant = lifeGrant;
			if (lifeGrant.isFile()) {
				kill(lifeGrant);
			}
			lifeGrant.delete();
			if (lifeGrant.exists()) {
				throw new RuntimeException("Cannot remove life grant file: " + lifeGrant.getPath());
			}
			FileOutputStream fos = new FileOutputStream(lifeGrant);
			socket = new ServerSocket(0, 10, InetAddress.getByAddress(new byte[] {127,0,0,42}));
			String token = vmname + "\n" + socket.getLocalPort() + " " + magic;
			fos.write(token.getBytes());
			fos.close();
			start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		try {
			while(true) {
				try {
					socket.setSoTimeout(400);
					Socket sock = socket.accept();
					if (verifyMagic(sock)) {
						break;
					}
				} catch (IOException e) {
					// ignore
				}
				try {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(lifeGrant)));
					String id = br.readLine();
					br.close();
					if (!vmname.equals(id)) {
						break;
					}
				}
				catch(IOException e) {
					// ignore
					break;
				}
			}
		}
		finally {
			System.err.println("Life grant revoked, terminating ...");
			System.err.flush();
			System.out.flush();
			Runtime.getRuntime().halt(0);
		}
	}

	private boolean verifyMagic(Socket sock) throws IOException {
		byte[] a = magic.getBytes();
		byte[] b = new byte[a.length];
		sock.setSoTimeout(500);
		int n = 0;
		while(n < a.length) {
			int m = sock.getInputStream().read(b, n, b.length - n);
			if (m < 0) {
				return false;
			}
			n += m;
		}
		return Arrays.equals(a, b);
	}
}
