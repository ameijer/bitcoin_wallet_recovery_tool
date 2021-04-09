package com.ameijer.bruteforce;

import java.io.PrintStream;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.SegwitAddress;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.bitcoinj.core.Address;
import java.io.PrintStream;
import org.bitcoinj.store.BlockStoreException;
import com.google.zxing.WriterException;
import java.util.Base64;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.File;

import java.nio.file.Files;
import java.nio.file.Path;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestIntegration {

    public static final String WALLET_FILE = "/tmp/wallet.dat";
    public static final String WIF_DIR = "/tmp/WIFs";
    private final String POSITIVE_BTC_ADDRESS = "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa";
    private final String POSITIVE_SEGWIT_ADDRESS = "bc1qp762gmkychywl4elnuyuwph68hqw0uc2jkzu3ax48zfjkskslpsq8p66gf";
    public static final Collection<String> POSITIVE_BALANCE_ADDRESSES =  ImmutableSet.of("17jRswg35d9wUejQccfrG8vdbmeQFRKP57");
    public static final Collection<String> POSITIVE_BALANCE_SEGWIT_ADDRESSES =  ImmutableSet.of("bc1q59sxjh8fl67d8rlq6nfaqcclld7v7pgetzlfmt");
    public static final Collection<String> ZERO_BALANCE_ADDRESSES =  ImmutableSet.of("13D8dHJU8ne4XBTdJJKwZQqmz6bFjGpA8K");
    
    @BeforeEach
    public void cleanFile() {
        try {
    	    File target = new File(WALLET_FILE);
    	    target.delete();
    	    target = new File(WIF_DIR);
    	    target.delete();
    	} catch (Exception e) {
    	    if ( e instanceof FileNotFoundException ){
    	        System.out.println(WALLET_FILE + " was not found, skipping deletion.." );
    	    } else throw e;
    	}
    }

    @Test
    @Order(6)
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
    @Order(5)
    public void testPositiveBalanceCheckerLegacy() throws IOException {
        
        // check known good address (one of satoshi's addresses)
        ArrayList<Address> goodAddress = new ArrayList<Address>();
        goodAddress.add(Address.fromString(MainNetParams.get(), POSITIVE_BTC_ADDRESS));
        Collection<String> results = Main.checkForPositiveBalances(goodAddress);
    	assertEquals(results.size(), 1);
    	assertTrue(results.contains(POSITIVE_BTC_ADDRESS));
    }
    
    @Test
    @Order(4)
    public void testPositiveBalanceCheckerSegwit() throws IOException {
        
        // check known good address (one of satoshi's addresses)
        ArrayList<Address> goodAddress = new ArrayList<Address>();
        goodAddress.add(Address.fromString(MainNetParams.get(), POSITIVE_SEGWIT_ADDRESS));
        List<String> results = Main.checkForPositiveBalances(goodAddress);
    	assertEquals(results.size(), 1);
    	assertTrue(results.contains(POSITIVE_SEGWIT_ADDRESS));
    }
    
	@Test
	@Order(1)
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
        
        File WIFdir = new File(WIF_DIR);
        assertTrue(WIFdir.exists());
        assertTrue(WIFdir.isDirectory());
        
        List<Path> result;
        try (Stream<Path> walk = Files.walk(WIFdir.toPath())) {
            result = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        
        assertEquals(result.size(), 0);
	}
	
	@Test
	@Order(7)
	public void testZeroBalWallet429Error() throws IOException, BlockStoreException, InterruptedException, WriterException, Exception {
		// call main with too low of a delay set, confirm that exception thrown with error
        
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
        String[] args = {"--wallet-file", WALLET_FILE, "--request-delay", "0"};
        IOException thrown = null;
        try {
            Main.main(args);
        } catch (IOException e){
            thrown = e;
        }
        
        assertNotNull(thrown);
        assertTrue(thrown.getMessage().contains("429"));
        
		
	}
	
	@Test
	@Order(2)
	public void testPosBalWallet() throws IOException, BlockStoreException, InterruptedException, WriterException, Exception {
	    runPositiveBalanceTest("POSITIVE_BALANCE_WALLET_CONTENTS", POSITIVE_BALANCE_ADDRESSES);
        assertWIFDirContents(POSITIVE_BALANCE_ADDRESSES);
	}
	

	@Test
	@Order(3)
	public void testPosBalSegwit() throws IOException, BlockStoreException, InterruptedException, WriterException, Exception {
        runPositiveBalanceTest("POSITIVE_BALANCE_SEGWIT_WALLET_CONTENTS", POSITIVE_BALANCE_SEGWIT_ADDRESSES);
        assertWIFDirContents(POSITIVE_BALANCE_SEGWIT_ADDRESSES);
	}
	
	private void assertWIFDirContents(Collection<String> containsPublic) throws IOException {
	    File WIFdir = new File(WIF_DIR);
        assertTrue(WIFdir.exists());
        assertTrue(WIFdir.isDirectory());
        
        List<Path> result;
        try (Stream<Path> walk = Files.walk(WIFdir.toPath())) {
            result = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
        
        for (Path toCheck : result){
            System.out.println("checking WIF file @ " + toCheck.toString());
            // for each file, decode it and confirm it contains the private key for one of the positive balance wallets
            String WIFdata = TestMain.readQRCode(toCheck.toFile());
            
            // decode from base
            ECKey toKey = ECKey.fromPrivate(DumpedPrivateKey.fromBase58(MainNetParams.get(), WIFdata).getKey().getPrivKeyBytes());
            
            String segString = SegwitAddress.fromKey(MainNetParams.get(), toKey).toBech32();
            String legString = LegacyAddress.fromKey(MainNetParams.get(), toKey).toBase58();
            
            System.out.println("derived " + segString + " and " + legString + " as positive balance addresses. checking...");
            assertTrue(containsPublic.contains(segString) || containsPublic.contains(legString));
            
        }
	}
	
	private void runPositiveBalanceTest(String walletContentsEnvVarName, Collection<String> toCheckColl) throws IOException, BlockStoreException, InterruptedException, WriterException, Exception {
	    // before running test, redirect stdout and stdin 
    	File file = File.createTempFile("test", null);
    	file.deleteOnExit();
    	System.out.println("Saving log output in: " + file.getAbsolutePath());
		PrintStream stringStream = new PrintStream(file.getAbsolutePath());
		PrintStream old = System.out;
        System.setOut(stringStream);
        
        // call main with bad wallet file, confirm io exception throws
		InputStream oldIn = System.in;
		// call main with no args, confirm help message printed
		
		String inputString = "foo\n" +  WALLET_FILE + "\n15\n";
		InputStream fakeIn = new ByteArrayInputStream(inputString.getBytes());
		System.setIn(fakeIn);
        
        // echo in 20k of corrupted wallet with zero bal addrress
        String walletContents = System.getenv(walletContentsEnvVarName);
        if (walletContents == null){
            String msg = "ERROR - THIS TEST REQUIRES THE BASE64 CONTENTS OF THE TEST WALLET TO BE STORED IN THE " + walletContentsEnvVarName + " ENV VAR";
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
        
        String[] args = new String[0];
        Main.main(args);
        
        System.setIn(oldIn);
        System.out.flush();
        System.setOut(old);
        
        String logOutput = Files.readString(file.toPath());
        System.out.println("program output: " + logOutput);
        
        for (String addr : toCheckColl ) {
            System.out.println("confirming program checked address: " + addr);
            assertTrue(logOutput.contains("checking address: " + addr));
        }
        
        //check for bad file handling
        assertTrue(logOutput.contains("invalid file path"));
	}

}
