package com.ameijer.bruteforce;

import org.junit.jupiter.api.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import java.awt.image.BufferedImage;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.MultiFormatReader;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.SegwitAddress;
import java.util.*;
import java.io.IOException;
import java.io.File;
import java.math.BigInteger;

import javax.imageio.ImageIO;

import static org.bitcoinj.core.Utils.HEX;
import org.bitcoinj.core.ECKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestMain {
    
    public static final String TEST_FILE_BASE_PATH = "/tmp/tests";
    private static ArrayList<ECKey> testKeypairs;
    private final String POSITIVE_BTC_ADDRESS = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";
    private final String POSITIVE_SEGWIT_ADDRESS = "bc1qp762gmkychywl4elnuyuwph68hqw0uc2jkzu3ax48zfjkskslpsq8p66gf";
    
    @BeforeEach
    public void prepKeys(){
      testKeypairs = new ArrayList<ECKey>();
      testKeypairs.add(ECKey.fromPrivate(new BigInteger(1, HEX.decode("180cb41c7c600be951b5d3d0a7334acc7506173875834f7a6c4c786a28fcbb19"))));
      testKeypairs.add(ECKey.fromASN1(HEX.decode("3082011302010104205c0b98e524ad188ddef35dc6abba13c34a351a05409e5d285403718b93336a4aa081a53081a2020101302c06072a8648ce3d0101022100fffffffffffffffffffffffffffffffffffffffffffffffffffffffefffffc2f300604010004010704410479be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798483ada7726a3c4655da4fbfc0e1108a8fd17b448a68554199c47d08ffb10d4b8022100fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141020101a144034200042af7a2aafe8dafd7dc7f9cfb58ce09bda7dce28653ab229b98d1d3d759660c672dd0db18c8c2d76aa470448e876fc2089ab1354c01a6e72cefc50915f4a963ee")));
    };
    

    @Test
    public void testBytesToHex() {
    	String result = Main.bytesToHex(new byte[]{0xa, 0x2, 0x7});
    	assertEquals("0A0207", result );
    }
    
    @Test
    public void testWIFQRGen() throws com.google.zxing.WriterException,IOException {
      File parent = new File(TEST_FILE_BASE_PATH);
      parent.mkdirs();
    	Set<File> results = Main.generateWIFQRs(parent, testKeypairs);
    	Set<String> expecteds = new HashSet<String>();
    	
    	for(ECKey expected: testKeypairs){
    	  System.out.println("adding expected: " + expected.getPrivateKeyAsWiF(MainNetParams.get()));
    	  expecteds.add(expected.getPrivateKeyAsWiF(MainNetParams.get()));
    	}
    	
    	for(File result : results){
    	  String expected = readQRCode(result);
    	  System.out.println("checking for address:" + expected);
    	  assertTrue(expecteds.contains(expected));
    	}
    }
    
    @Test
    public void test0BalanceChecker() throws IOException {
    	ArrayList<Address> generated = new ArrayList<Address>();
    	
    	for(int i = 0; i< 10; i++){
		    ECKey toCheck = new ECKey();
		    generated.add(LegacyAddress.fromKey(MainNetParams.get(), toCheck));
		    generated.add(SegwitAddress.fromKey(MainNetParams.get(), toCheck));
    	}
    	// check random addresses, these will be 0
    	List<String> results = Main.checkForPositiveBalances(generated);
    	assertEquals(results.size(), 0);
    }
    
    @Test
    public void testPositiveBalanceCheckerLegacy() throws IOException {
        
        // check known good address (one of satoshi's addresses)
        ArrayList<Address> goodAddress = new ArrayList<Address>();
        goodAddress.add(Address.fromString(MainNetParams.get(), POSITIVE_BTC_ADDRESS));
        List<String> results = Main.checkForPositiveBalances(goodAddress);
    	assertEquals(results.size(), 1);
    	assertTrue(results.contains(POSITIVE_BTC_ADDRESS));
    }
    
    @Test
    public void testPositiveBalanceCheckerSegwit() throws IOException {
        
        // check known good address (one of satoshi's addresses)
        ArrayList<Address> goodAddress = new ArrayList<Address>();
        goodAddress.add(Address.fromString(MainNetParams.get(), POSITIVE_SEGWIT_ADDRESS));
        List<String> results = Main.checkForPositiveBalances(goodAddress);
    	assertEquals(results.size(), 1);
    	assertTrue(results.contains(POSITIVE_SEGWIT_ADDRESS));
    }
    
    
    private String readQRCode(File qrCodeFile) {
        String encodedContent = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(qrCodeFile);

            encodedContent = readQRCode(bufferedImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encodedContent;
    }
    
    private String readQRCode(BufferedImage bufferedImage) {
	    String encodedContent = null;
	    try {
		    BufferedImageLuminanceSource bufferedImageLuminanceSource = new BufferedImageLuminanceSource(bufferedImage);
		    HybridBinarizer hybridBinarizer = new HybridBinarizer(bufferedImageLuminanceSource);
		    BinaryBitmap binaryBitmap = new BinaryBitmap(hybridBinarizer);
		    MultiFormatReader multiFormatReader = new MultiFormatReader();

		    Result result = multiFormatReader.decode(binaryBitmap);
		    encodedContent = result.getText();
	    } catch (Exception e) {
		    e.printStackTrace();
		    return null;
	    }
	    return encodedContent;
	}

}
