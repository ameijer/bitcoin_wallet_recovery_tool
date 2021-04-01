package com.ameijer.bruteforce;

import java.io.PrintStream;
import java.io.FileDescriptor;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.io.IOException;
import org.bitcoinj.store.BlockStoreException;
import com.google.zxing.WriterException;

import org.junit.jupiter.api.*;

public class TestIntegration {

    public static final String WALLET_FILE_DIR = "/tmp/wallet.dat";
    public static final Collection<String> POSITIVE_BALANCE_ADDRESSES =  ImmutableSet.of("bc1q59sxjh8fl67d8rlq6nfaqcclld7v7pgetzlfmt",
        "3DwXtDCUFLKTnsDajhdn9SZ5kk3sTnR7z3", "17jRswg35d9wUejQccfrG8vdbmeQFRKP57");
    public static final Collection<String> ZERO_BALANCE_ADDRESSES =  ImmutableSet.of("13D8dHJU8ne4XBTdJJKwZQqmz6bFjGpA8K");
    
	@Test
	public void testWallet() throws IOException, BlockStoreException, InterruptedException, WriterException {
		// before running test, redirect stdout 
		//final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	//final String utf8 = StandardCharsets.UTF_8.name();
		//PrintStream stringStream = new PrintStream(baos, true, utf8);
        //System.setOut(stringStream);
        
        // decrypt wallet using secret stored in env var
        
        // invoke main method on wallet
        String[] args = {"--wallet-file", WALLET_FILE_DIR, "--request-delay", "15"};
        Main.main(args);
        
        // obtain string output
        //String logOutput = baos.toString(utf8);
        
        // point back at console after test
        //PrintStream consoleStream = new PrintStream(new FileOutputStream(FileDescriptor.out));
        //System.setOut(consoleStream);
        
        //System.out.println("program output: " + logOutput);
	}
	
}
