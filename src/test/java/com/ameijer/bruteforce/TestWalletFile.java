package com.ameijer.bruteforce;

import org.junit.jupiter.api.*;
import java.io.File;
import java.io.FileWriter;
import java.util.UUID;
import java.io.IOException;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestWalletFile {

    public static final String TEST_FILE_PATH = "/tmp/testwallet.dat";
    public static final String TEST_FILE_CONTENTS = UUID.randomUUID().toString() + UUID.randomUUID().toString();
    private WalletFile underTest;
    private Random random = new Random();
    
    @BeforeEach
    public void setupFile() throws IOException {
    	FileWriter target = new FileWriter(TEST_FILE_PATH);
    	target.write(TEST_FILE_CONTENTS);
      	target.close();
      	
      	File testWallet = new File(TEST_FILE_PATH);
    	underTest = new WalletFile(testWallet);
    }
    
    @AfterEach
    public void cleanFile() throws IOException {
    	File target = new File(TEST_FILE_PATH);
    	target.delete();
    }
    
    @Test
    public void testLength() {
    	System.out.println("total file length: " + underTest.getTotalPos() + ", expected: " + TEST_FILE_CONTENTS.length());
        assertEquals(underTest.getTotalPos(), TEST_FILE_CONTENTS.length());
    }
    
    @Test
    public void testPosition() {
    	assertEquals(underTest.getPosition(), 0);
    	
    	int advancePosition = 1 + random.nextInt(TEST_FILE_CONTENTS.length() - 1);
    	System.out.println("Advancing " + advancePosition + " positions into the file");
    	for(int i = 0; i < advancePosition; i++){
    	  underTest.nextCandidate();
    	  System.out.println("Advancing...");
    	}
    	
    	assertEquals(underTest.getPosition(), advancePosition);
    }
    
    @Test
    public void testNextCandidate()  {
    	int keyCount = 0;
    	byte[] candidate = underTest.nextCandidate();
    	do {
    	  String candidateString = new String(candidate);
    	  System.out.println("examining candidate: " + candidateString);
    	  assertEquals(TEST_FILE_CONTENTS.substring(keyCount,keyCount + 32), candidateString);
    	  candidate = underTest.nextCandidate();
    	  keyCount++;
    	} while (candidate != null);
    }

}
