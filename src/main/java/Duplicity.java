import java.io.*;
import java.nio.file.Files;
import java.security.*;
import java.util.*;

public class Duplicity {
	private static class Hasher extends Thread {
		File file;
		Map<String, List<File>> hashes;
		public Hasher(File file, Map<String, List<File>> hashes) {
			this.file = file;
			this.hashes = hashes;
		}
		@Override
		public void run() {
			if (file.isFile()) {
				// Pro bezny soubor spocitame hash.
				// Pri chybe budeme soubor ignorovat (neni nejlepsi reseni,
				// ale tady bude fungovat rozumne).
				String hash;
				try {
					hash = getFileHash(file);
				} catch (IOException e) {
					return;
				} catch (NoSuchAlgorithmException e2) {
					return;
				}
				List<File> similar = hashes.get(hash);
				if (similar == null) {
					similar = new ArrayList<>();
					hashes.put(hash, similar);
				}
				similar.add(file);
			} else if (file.isDirectory()) {
				// Pro adresare projdeme vsechny soubory v nem.
				File[] files = file.listFiles();
				ArrayList<Hasher> hashers = new ArrayList<Hasher>();
				for (File f : files) {
					hashers.add(new Hasher(f, hashes));
					hashers.get(hashers.size() - 1).start();
				}
				for (Hasher h : hashers) {
					try {
						h.join();
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	private static class Comparator extends Thread {
		File af;
		File bf;
		public Comparator(File af,  File bf) {
			this.af = af;
			this.bf = bf;
		}
		@Override
		public void run() {
			DEBUG("Comparing %s and %s.", af, bf);
	 
			try {
				InputStream a = new FileInputStream(af);
				InputStream b = new FileInputStream(bf);
				while (true) {
					int aByte = a.read();
					int bByte = b.read();
					if ((aByte == -1) && (bByte == -1)) {
						System.out.printf("%s and %s have the same content\n", af, bf);
						break;
					}
					if (aByte != bByte) {
						break;
					}
				}
				a.close();
				b.close();
				return;
			} catch (IOException e) {
			} finally {
			}
		}
	}

	private static class EntryAnalyzer extends Thread {
		Map.Entry<String, List<File>> entry;
		public EntryAnalyzer(Map.Entry<String, List<File>> entry) {
			this.entry = entry;
		}
		@Override
		public void run() {
			File[] similar = entry.getValue().toArray(new File[0]);
 
			if (similar.length == 1) {
				return;
			}
 
			ArrayList<Comparator> comparators = new ArrayList<Comparator>();

			for (int i = 0; i < similar.length - 1; i++) {
				for (int j = i + 1; j < similar.length; j++) {
					try {
						// Stejna cesta?
						if (similar[i].getCanonicalPath().equals(similar[j].getCanonicalPath())) {
							continue;
						}
 
						// Porovname bajt po bajtu
						comparators.add(new Comparator(similar[i], similar[j]));
						comparators.get(comparators.size() - 1).start();
					} catch (IOException e) {
					}
				}
			}
			for (Comparator comparator : comparators) {
				try {
					comparator.join();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public static void main(String[] args) {
		// Mapovani mezi hashem (MD5) a soubory se stejnym hashem.
		Map<String, List<File>> hashes = new HashMap<String, List<File>>();
		ArrayList<Hasher> hashers = new ArrayList<Hasher>();

		// Spocitame hashe pro kazdy soubor (nebo rekurzivne pro adresare).
		for (String filename : args) {
			hashers.add(new Hasher(new File(filename), hashes));
			hashers.get(hashers.size() - 1).start();
		}
		try {
			for (Hasher h : hashers) {
				h.join();
			}
		} catch (InterruptedException e) {
		}
		ArrayList<EntryAnalyzer> entryanalyzers = new ArrayList<EntryAnalyzer>();
		for (Map.Entry<String, List<File>> entry : hashes.entrySet()) {
			entryanalyzers.add(new EntryAnalyzer(entry));
			entryanalyzers.get(entryanalyzers.size() - 1).run();
		}
	}

	private static String getFileHash(File f) throws IOException, NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
 
		InputStream is = new FileInputStream(f);
 
		byte[] data = new byte[1024];
 
		while (true) {
			int actuallyRead = is.read(data);
			if (actuallyRead == -1) {
				break;
			}
			digest.update(data, 0, actuallyRead);
		}
 
		is.close();
 
		byte[] digestBytes = digest.digest();
 
		StringBuffer sb = new StringBuffer();
		for (byte b : digestBytes) {
			sb.append(String.format("%02x", b));
		}
 
		String hash = sb.toString();
 
		DEBUG("%s => %s", f.getName(), hash);
 
		return hash;
	}

	private static void DEBUG(String fmt, Object... args) {
		// System.out.printf("[debug]: " + fmt + "\n", args);
	}
}
