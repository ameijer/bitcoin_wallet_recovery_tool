package com.ameijer.bruteforce;

import java.io.PrintStream;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.io.IOException;
import org.bitcoinj.store.BlockStoreException;
import com.google.zxing.WriterException;
import java.util.Base64;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;
import java.nio.file.Files;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestIntegration {

    public static final String WALLET_FILE = "/tmp/wallet.dat";
    public static final Collection<String> POSITIVE_BALANCE_ADDRESSES =  ImmutableSet.of("bc1q59sxjh8fl67d8rlq6nfaqcclld7v7pgetzlfmt", "17jRswg35d9wUejQccfrG8vdbmeQFRKP57");
    public static final Collection<String> ZERO_BALANCE_ADDRESSES =  ImmutableSet.of("13D8dHJU8ne4XBTdJJKwZQqmz6bFjGpA8K");
    
    @BeforeEach
    public void cleanFile() {
        try {
    	    File target = new File(WALLET_FILE);
    	    target.delete();
    	} catch (Exception e) {
    	    if ( e instanceof FileNotFoundException ){
    	        System.out.println(WALLET_FILE + " was not found, skipping deletion.." );
    	    } else throw e;
    	}
    }
    
	@Test
	public void testZeroBalWallet() throws IOException, BlockStoreException, InterruptedException, WriterException, Exception {
		// before running test, redirect stdout 
    	File file = File.createTempFile("test", null);
    	file.deleteOnExit();
    	System.out.println("Saving log output in: " + file.getAbsolutePath());
		PrintStream stringStream = new PrintStream(file.getAbsolutePath());
		PrintStream old = System.out;
        System.setOut(stringStream);
        
        // echo in 20k of corrupted wallet with zero bal addrress
        String walletContents = System.getenv("ZERO_BALANCE_WALLET_CONTENTS");
        if (walletContents == null){
            String msg = "ERROR - THIS TEST REQUIRES THE BASE64 CONTENTS OF THE TEST WALLET TO BE STORED IN THE \'ZERO_BALANCE_WALLET_CONTENTS\' ENV VAR";
            System.out.println(msg);
            throw new Exception(msg);
        }
        
        try ( FileOutputStream fos = new FileOutputStream(WALLET_FILE); ) {
          byte[] decoder = Base64.getDecoder().decode(walletContents);

          fos.write(decoder);
        } catch (Exception e) {
          e.printStackTrace();
          throw e;
        }
        
        // invoke main method on wallet
        String[] args = {"--wallet-file", WALLET_FILE, "--request-delay", "15"};
        Main.main(args);
        
        // point back at console after test
        stringStream.flush();
        System.out.flush();
        System.setOut(old);
        
        String logOutput = Files.readString(file.toPath());
        System.out.println("program output: " + logOutput);
        
        for (String addr : ZERO_BALANCE_ADDRESSES ) {
            System.out.println("confirming program checked address: " + addr);
            assertTrue(logOutput.contains("checking address: " + addr));
        }
	}
	
	@Test
	public void testPosBalWallet() throws IOException, BlockStoreException, InterruptedException, WriterException {
	
	}
	
	@Test
	public void testPosBalSegwit() throws IOException, BlockStoreException, InterruptedException, WriterException {
	}
}
