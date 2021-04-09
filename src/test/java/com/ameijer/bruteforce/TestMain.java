package com.ameijer.bruteforce;

import org.junit.jupiter.api.*;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
import com.google.zxing.WriterException;
import java.util.*;
import org.bitcoinj.store.BlockStoreException;
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
    public void testExtractValidKeys() {
        ArrayList<String> generated = new ArrayList<String>();
        ArrayList<ECKey> keysUsed = new ArrayList<ECKey>();
        int i = 0;
        for(; i< 10; i++){
		    ECKey toCheck = new ECKey();
		    generated.add(LegacyAddress.fromKey(MainNetParams.get(), toCheck).toBase58());
		    generated.add(SegwitAddress.fromKey(MainNetParams.get(), toCheck).toBech32());
		    keysUsed.add(toCheck);
		    
    	}
    	
    	Collection<ECKey> valids = Main.extractAllValidKeys(generated, keysUsed);
    	
    	assertEquals(valids.size(), i);
    	assertTrue(valids.containsAll(keysUsed));
    }
    
    @Test
	public void testHelpMessagePrinted() throws IOException, BlockStoreException, InterruptedException, WriterException {
	   
		// call main with bad args, confirm help message printed
		PrintStream oldOut = System.out;
		ByteArrayOutputStream str = new ByteArrayOutputStream();
        System.setOut(new PrintStream(str));
		// invoke main method on wallet
        String[] args = {"foo"};
        try {
            Main.main(args);
        } catch (IOException e){
            assertTrue(e.getMessage().contains("unknown arg"));
        }
        System.out.flush();
        String output = str.toString();
        System.setOut(oldOut);
        
        System.out.println("program output: " + output);
        assertTrue(output.contains("--wallet-file"));
        assertTrue(output.contains("--request-delay"));
	}
    
    protected static String readQRCode(File qrCodeFile) {
        String encodedContent = null;
        try {
            BufferedImage bufferedImage = ImageIO.read(qrCodeFile);

            encodedContent = readQRCode(bufferedImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return encodedContent;
    }
    
    private static String readQRCode(BufferedImage bufferedImage) {
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
