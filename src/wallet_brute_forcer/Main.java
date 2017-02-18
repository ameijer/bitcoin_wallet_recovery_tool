package wallet_brute_forcer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.imageio.ImageIO;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.store.BlockStoreException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * 
 * Main method class to execute interactive wallet brute forcer. 
 * 
 * @author alex.meijer
 *
 */
public class Main {

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	/**
	 * Helper method to convert bytes to hex representation
	 * 
	 * @param bytes The bytes to convert to Hex
	 * @return the hex string of the input bytes
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Main method. accepts wallet file name in args. 
	 * 
	 * @param args
	 * @throws IOException
	 * @throws BlockStoreException
	 * @throws InterruptedException
	 * @throws WriterException
	 */
	public static void main(String[] args)
			throws IOException, BlockStoreException, InterruptedException, WriterException {

		System.out.println("Welcome to the wallet brute forcer.\n Crafted with <3 in Arlington, VA.");

		File inputDat = null;
		boolean exists = false;
		if (args.length == 1) {
			inputDat = new File(args[0]);
			exists = inputDat.exists();
		}

		//read wallet file
		while (!exists) {
			System.out.print("Enter the path to your wallet.dat file: ");

			Scanner scan = new Scanner(System.in);

			String path = scan.nextLine();
			scan.close();
			inputDat = new File(path);

			exists = inputDat.exists();

			if (!exists) {
				System.out.println("sorry, invalid file path.");
			}
		}

		WalletFile myFile = new WalletFile(inputDat);

		List<ECKey> keys = new ArrayList<ECKey>();

		byte[] keyContents = new byte[10];

		//scan through the file, byte-by-byte. Toss out invalid elliptic curve keys
		while (keyContents != null) {
			ECKey candidateKey = null;
			int currentPos = myFile.getPosition();
			int totalPos = myFile.getTotalPos();
			keyContents = myFile.nextCandidate();
			System.out.println("read in " + currentPos + "/" + totalPos + " bytes");
			try {
				candidateKey = ECKey.fromPrivate(keyContents);
				keys.add(candidateKey);

			} catch (Exception e) {
			}

		}

		// array is full of potentially 100k+ key candidates

		ArrayList<Address> toCheck = new ArrayList<Address>();

		Iterator<ECKey> iter = keys.iterator();
		while (iter.hasNext()) {

			toCheck.add(iter.next().toAddress(MainNetParams.get()));

		}

		//much larger than 150 addresses at a time exceeds HTTP URL length limits
		List<List<Address>> groups = Lists.partition(toCheck, 150);

		List<String> allValids = new ArrayList<String>();
		int numScanned = 0;
		for (List<Address> group : groups) {
			List<String> validAddresses = checkForPositiveBalances(group);
			numScanned += group.size();
			System.out.println("Scanned " + numScanned + "/" + toCheck.size() + " addresses using web API");
			Thread.sleep(100);
			allValids.addAll(validAddresses);
		}

		List<ECKey> validKeypairs = extractAllValidKeys(allValids, keys);

		//Generate WIF qr codes that can be scanned by wallet software
		File WIFoutputDir = new File(inputDat.getParentFile(), "WIFs");
		WIFoutputDir.mkdirs();

		Set<File> generated = generateWIFQRs(WIFoutputDir, validKeypairs);
		System.out.println("done. generated " + generated.size() + " QRs to scan");

	}

	private static Set<File> generateWIFQRs(File wIFoutputDir, List<ECKey> validKeypairs)
			throws WriterException, IOException {

		Set<File> QRs = new HashSet<File>(); 
		for (ECKey keypair : validKeypairs) {
			String addr = keypair.toAddress(MainNetParams.get()).toBase58();
			File outputFile = new File(wIFoutputDir, addr + ".png");
			createQRImage(outputFile, keypair.getPrivateKeyAsWiF(MainNetParams.get()), 512, "png");
			QRs.add(outputFile); 
		}

		return QRs; 
	}

	private static void createQRImage(File qrFile, String qrCodeText, int size, String fileType)
			throws WriterException, IOException {
		// Create the ByteMatrix for the QR-Code that encodes the given String
		Hashtable hintMap = new Hashtable();
		hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix byteMatrix = qrCodeWriter.encode(qrCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
		// Make the BufferedImage that are to hold the QRCode
		int matrixWidth = byteMatrix.getWidth();
		BufferedImage image = new BufferedImage(matrixWidth, matrixWidth, BufferedImage.TYPE_INT_RGB);
		image.createGraphics();

		Graphics2D graphics = (Graphics2D) image.getGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, matrixWidth, matrixWidth);
		// Paint and save the image using the ByteMatrix
		graphics.setColor(Color.BLACK);

		for (int i = 0; i < matrixWidth; i++) {
			for (int j = 0; j < matrixWidth; j++) {
				if (byteMatrix.get(i, j)) {
					graphics.fillRect(i, j, 1, 1);
				}
			}
		}
		ImageIO.write(image, fileType, qrFile);
	}

	private static List<ECKey> extractAllValidKeys(List<String> allValids, List<ECKey> keys) {
		ArrayList<ECKey> valids = new ArrayList<ECKey>();
		for (String valid : allValids) {

			for (ECKey keypair : keys) {
				if (keypair.toAddress(MainNetParams.get()).toBase58().equals(valid)) {
					valids.add(keypair);
				}
			}

		}

		return valids;
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	/**
	 * Leverage blockchain.info multi-address REST api to quickly check balances
	 * 
	 * @param group the list of address objects to check. should not be more than ~150 addresses
	 * @return The base58 string representation of any addresses in the input list with positive addresses 
	 * @throws JSONException
	 * @throws IOException
	 */
	private static List<String> checkForPositiveBalances(List<Address> group) throws JSONException, IOException {

		StringBuilder urlBuilder = new StringBuilder("https://blockchain.info/multiaddr?active=");

		for (Address addr : group) {
			urlBuilder.append(addr.toBase58());
			urlBuilder.append("|");
		}

		urlBuilder.deleteCharAt(urlBuilder.length() - 1);
		urlBuilder.append("&limit=1");

		JSONObject results = readJsonFromUrl(urlBuilder.toString());

		JSONArray addrs = results.getJSONArray("addresses");

		ArrayList<String> stringResults = new ArrayList<String>();
		for (int i = 0; i < addrs.length(); i++) {
			JSONObject addrObj = addrs.getJSONObject(i);

			if (addrObj.getLong("final_balance") > 0) {
				stringResults.add(addrObj.getString("address"));
			}
		}

		return stringResults;
	}

}
