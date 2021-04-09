package com.ameijer.bruteforce;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Helper class representing a wallet file. tracks the position within a file,
 * and returns 256 bit private key candidates as it scans through the file
 * sequentially
 * 
 * @author alex.meijer
 *
 */
public class WalletFile {

	private final byte[] contents;

	private int position = 0;

	public WalletFile(File input) throws IOException {
		contents = Files.readAllBytes(input.toPath());
	}

	/**
	 * Bitcoin private keys are 32 bytes long. We know they are hidden somewhere
	 * in the file, as long as they themselves are not corrupted returns null
	 * when there are no more candidates.
	 * 
	 * @return 32 bytes representing the next private key candidate from the wallet file
	 */
	public byte[] nextCandidate() {
		byte[] nextKey = new byte[32];

		int startPosition = position++;
		try {
			for (int i = 0; i < nextKey.length; i++) {
				nextKey[i] = contents[startPosition++];
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			return null;
		}

		return nextKey;
	}

	public int getPosition() {
		return position;
	}

	public int getTotalPos() {
		return contents.length;
	}
}
