/*
 * Copyright © 2017 Ron de Jong (ronuitzaandam@gmail.com).
 *
 * This is free software; you can redistribute it 
 * under the terms of the Creative Commons License
 * Creative Commons License: (CC BY-NC-ND 4.0) as published by
 * https://creativecommons.org/licenses/by-nc-nd/4.0/ ; either
 * version 4.0 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * Creative Commons Attribution-NonCommercial-NoDerivatives 4.0
 * International Public License for more details.
 *
 * You should have received a copy of the Creative Commons 
 * Public License License along with this software;
 */

package rdj;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimerTask;

public class FinalCrypt extends Thread
{
    public static boolean verbose = false;
//    private boolean debug = false, print = false, symlink = false, txt = false, bin = false, dec = false, hex = false, chr = false, dry = false;
    private boolean symlink = false, txt = false, dry = false;
    private static boolean print = false, bin = false, dec = false, hex = false, chr = false;

    private final int BUFFERSIZEDEFAULT = (1 * 1024 * 1024); // 1MB BufferSize overall better performance
    private int bufferSize = BUFFERSIZEDEFAULT; // Default 1MB
    private int readTargetSourceBufferSize;
    private int readKeySourceBufferSize;
    private int wrteTargetDestinBufferSize;

    private int printAddressByteCounter = 0;
    private final UI ui;
    
    private TimerTask updateProgressTask;
    private java.util.Timer updateProgressTaskTimer;

    private boolean stopPending = false;
    private static boolean pausing = false;
    public boolean processRunning = false;

    private boolean targetSourceEnded;
//							1234567890123456789012345678901234567890123456789012345678901234567890
    public static final String FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE = "FinalCrypt - File Encryption Program - Plain Text Authentication Token";
    private Calendar	startCalendar;
    private Calendar	processProgressCalendar;
    private static double   bytesPerMilliSecond = 0;
//											❌ ❎ 🚫 ⊝ ⊖⭕⛔ ⨷ 🆘 ☝ ☹ 💣 🔐 🔏 📄 XOR ⊕ XOR ⊻ 🔀 ☒ ✓ ✔ ■ ▣ Ⅱ Ⅱ  🔓->🔒->🔓 ⎘ ✔ ⚛
//											🡔 🡕 🡖 🡗 | 🡤 🡥 🡦 🡧 | 🡬 🡭 🡮 🡯 | 🡴 🡵 🡶 🡷 | 🡼 🡽 🡾 🡿 | 🢄 🢅 🢆 🢇 | ⬈ ⬉ ⬊ ⬋ | ⇖ ⇗ ⇘ ⇙ | ↖ ↗ ↘ ↙
    public static final String UTF8_ENCRYPT_SYMBOL =		    "🔒";
    public static final String UTF8_XOR_NOMAC_SYMBOL =	    "🔀";
    public static final String UTF8_UNENCRYPTABLE_SYMBOL =	    "⚠";

    public static final String UTF8_ENCRYPT_DESC =		    "Encrypt";
    public static final String UTF8_XOR_NOMAC_DESC =	    "XOR";
    public static final String UTF8_UNENCRYPTABLE_DESC =	    "Unencryptable";

    public static final String UTF8_DECRYPT_SYMBOL =		    "🔓";
    public static final String UTF8_UNDECRYPTABLE_SYMBOL =	    "⚠";
    public static final String UTF8_DECRYPT_DESC =		    "Decrypt";
    public static final String UTF8_UNDECRYPTABLE_DESC =	    "Undecryptable";

    public static 	String UTF8_PROCESS_SYMBOL =		    "?";

    public static final String UTF8_CLONE_SYMBOL =		    "℄";
    public static final String UTF8_DELETE_SYMBOL =		    "🗑";

    public static final String UTF8_CLONE_DESC =		    "Clone";
    public static final String UTF8_DELETE_DESC =		    "Delete";

    public static final String UTF8_FINISHED_SYMBOL =		    "✔";

    public static final String UTF8_FINISHED_DESC =		    "Finished";
    
    public static final String UTF8_PAUSE_SYMBOL =		    "Ⅱ";
    public static final String UTF8_STOP_SYMBOL =		    "■";

    public static final String UTF8_PAUSE_DESC =		    "Pause";
    public static final String UTF8_STOP_DESC =			    "Stop";

    public boolean disableMAC = false; // Disable Message Authentication Mode DANGEROUS
    
    private static String pwd = ""; // abc = 012
    private static int pwdPos = 0;
    public static final String HASH_ALGORITHM_NAME = "SHA-256"; // SHA-1 SHA-256 SHA-384 SHA-512
    private static String printString;
    private long lastBytesProcessed;

    public FinalCrypt(UI ui)
    {   
//      Set the locations of the version resources
        readTargetSourceBufferSize =	bufferSize;
        readKeySourceBufferSize =	bufferSize;
        wrteTargetDestinBufferSize =	bufferSize;        
        this.ui = ui;
//        fc = this;
    }
        
    public int getBufferSize()                                              { return bufferSize; }
    
//    public boolean getDebug()                                               { return debug; }
    public boolean getVerbose()                                             { return verbose; }
    public boolean getPrint()                                               { return print; }
    public boolean getSymlink()                                             { return symlink; }
    public boolean getTXT()                                                 { return txt; }
    public boolean getBin()                                                 { return bin; }
    public boolean getDec()                                                 { return dec; }
    public boolean getHex()                                                 { return hex; }
    public boolean getChr()                                                 { return chr; }
    public boolean getDry()                                                 { return dry; }
    public int getBufferSizeDefault()					    { return BUFFERSIZEDEFAULT; }
//    public ArrayList<Path> getTargetFilesPathList()                         { return targetReadFilesPathList; }
//    public Path getKeyFilePath()                                         { return keyReadFilePath; }
//    public Path getOutputFilePath()                                         { return targetDestinPath; }
    
//    public void setDebug(boolean debug)                                     { this.debug = debug; }
    public void setVerbose(boolean verbose)                                 { this.verbose = verbose; }
    public void setPrint(boolean print)                                     { this.print = print; }
    public void setSymlink(boolean symlink)                                 { this.symlink = symlink; }
    public void setTXT(boolean txt)                                         { this.txt = txt; }
    public void setBin(boolean bin)                                         { this.bin = bin; }
    public void setDec(boolean dec)                                         { this.dec = dec; }
    public void setHex(boolean hex)                                         { this.hex = hex; }
    public void setChr(boolean chr)                                         { this.chr = chr; }
    public void setDry(boolean dry)                                         { this.dry = dry; }
    public void setBufferSize(int bufferSize)                               
    {
        this.bufferSize = bufferSize;
        readTargetSourceBufferSize = this.bufferSize; 
        readKeySourceBufferSize = this.bufferSize; 
        wrteTargetDestinBufferSize = this.bufferSize;
    }
        
    public void encryptSelection
    (
	    FCPathList targetSourceFCPathList
	    , FCPathList filteredTargetSourceFCPathList // encryptableList / decryptableList
	    , FCPath keySourceFCPath
	    , boolean encryptmode
	    , String pwdParam
	    , boolean open // Opens targets after finishing
    )// throws InterruptedException
    {
	if (pwdParam.length() > 0) { pwd = pwdParam; } else { pwd = ""; }

	startCalendar = Calendar.getInstance(Locale.ROOT);

	if ( keySourceFCPath.size < bufferSize ) { setBufferSize((int)keySourceFCPath.size); }
	
        Stats allDataStats = new Stats(); allDataStats.reset();
        
        Stat readTargetSourceStat = new Stat(); readTargetSourceStat.reset();
//        Stat readKeySourceStat = new Stat(); readKeySourceStat.reset();
//        Stat wrteTargetDestinStat = new Stat(); wrteTargetDestinStat.reset();
//        Stat readTargetDestinStat = new Stat(); readTargetDestinStat.reset();
        Stat wrteTargetSourceStat = new Stat(); wrteTargetSourceStat.reset();
        
        stopPending = false;
        pausing = false;
	processRunning = true;

        // Get TOTALS
        allDataStats.setFilesTotal(filteredTargetSourceFCPathList.encryptableFiles + filteredTargetSourceFCPathList.decryptableFiles);
        allDataStats.setAllDataBytesTotal(filteredTargetSourceFCPathList.encryptableFilesSize + filteredTargetSourceFCPathList.decryptableFilesSize);
	String modeDesc = "";
	if (encryptmode)
	{
	    if ( ! disableMAC ) { modeDesc = "encrypting"; } else { modeDesc = "encrypting (legacy)"; }
	}
	else
	{
	    if ( ! disableMAC ) { modeDesc = "decrypting"; } else { modeDesc = "decrypting (legacy)"; }
	}
	ui.log(allDataStats.getStartSummary(modeDesc), true, true, true, false, false);
        try { Thread.sleep(100); } catch (InterruptedException ex) {  }
        
//      Setup the Progress TIMER & TASK
        updateProgressTask = new TimerTask() { private long bytesTotal;
	private long bytesProcessed;
	@Override public void run()
        {
	    long fileBytesProcessed =	(readTargetSourceStat.getFileBytesProcessed() + wrteTargetSourceStat.getFileBytesProcessed());
	    double fileBytesPercent =	((readTargetSourceStat.getFileBytesTotal()) / 100.0); //  1000 / 100 = (long)10     10 > 0.1 (10*0.01)
	    int fileBytesPercentage =	(int)(fileBytesProcessed / fileBytesPercent); // 600 / 10 = 60 - 600 * (10*0.01)
	    
	    long filesBytesProcessed =	(allDataStats.getFilesBytesProcessed());
	    double filesBytesPercent =	((allDataStats.getFilesBytesTotal() ) / 100.0);
	    int filesBytesPercentage =	(int)(filesBytesProcessed / filesBytesPercent);

	    processProgressCalendar = Calendar.getInstance(Locale.ROOT);
	    bytesTotal =	    allDataStats.getFilesBytesTotal();
	    bytesProcessed =	    allDataStats.getFileBytesProcessed();
	    bytesPerMilliSecond =   filesBytesProcessed / (processProgressCalendar.getTimeInMillis() - startCalendar.getTimeInMillis());
	    lastBytesProcessed = bytesProcessed;
            ui.processProgress( fileBytesPercentage, filesBytesPercentage, bytesTotal, bytesProcessed, bytesPerMilliSecond );
	    
        }}; updateProgressTaskTimer = new java.util.Timer(); updateProgressTaskTimer.schedule(updateProgressTask, 100L, 100L);


//      Start Files Encryption Clock
        allDataStats.setAllDataStartNanoTime();
        
        // Encrypt Files loop
	
	encryptTargetloop: for (Iterator it = filteredTargetSourceFCPathList.iterator(); it.hasNext();)
	{
	    pwdPos = 0;
	    MessageDigest srcMessageDigest = null; try { srcMessageDigest = MessageDigest.getInstance(FinalCrypt.HASH_ALGORITHM_NAME); } catch (NoSuchAlgorithmException ex) { ui.log("Error: NoSuchAlgorithmException: MessageDigest.getInstance(\"SHA-2\")\r\n", false, true, true, true, false);}
	    MessageDigest dstMessageDigest = null; try { dstMessageDigest = MessageDigest.getInstance(FinalCrypt.HASH_ALGORITHM_NAME); } catch (NoSuchAlgorithmException ex) { ui.log("Error: NoSuchAlgorithmException: MessageDigest.getInstance(\"SHA-2\")\r\n", false, true, true, true, false);}
	    
	    FCPath newTargetSourceFCPath = (FCPath) it.next();
	    FCPath oldTargetSourceFCPath = newTargetSourceFCPath.clone(newTargetSourceFCPath);
	    Path targetDestinPath = null;
	    String fileStatusLine = "";
            if (stopPending) { targetSourceEnded = true; break encryptTargetloop; }
	    if ((newTargetSourceFCPath.path.compareTo(keySourceFCPath.path) != 0))
	    {
//		Determine extension ===========================================================================================================================================================================
		
		String bit_extension =	    ".bit";
		int lastDotPos =    newTargetSourceFCPath.path.getFileName().toString().lastIndexOf('.'); // -1 no extension
		int lastPos =	    newTargetSourceFCPath.path.getFileName().toString().length();
		String extension =  ""; if (lastDotPos != -1) { extension = newTargetSourceFCPath.path.getFileName().toString().substring(lastDotPos, lastPos); } else { extension = ""; }

//		Set new name of target destination

		if ( ! disableMAC)
		{
		    if	(encryptmode)				{ UTF8_PROCESS_SYMBOL = UTF8_ENCRYPT_SYMBOL; targetDestinPath = newTargetSourceFCPath.path.resolveSibling(newTargetSourceFCPath.path.getFileName().toString() + bit_extension); }
		    else // (decryptmode)
		    {
			UTF8_PROCESS_SYMBOL = UTF8_DECRYPT_SYMBOL;
			if (extension.equals(bit_extension))	{ targetDestinPath = Paths.get(newTargetSourceFCPath.path.toString().substring(0, newTargetSourceFCPath.path.toString().lastIndexOf('.'))); }
			else					{ targetDestinPath = newTargetSourceFCPath.path.resolveSibling(newTargetSourceFCPath.path.getFileName().toString() + bit_extension); }
		    }
		}
		else // Disable Message Authentication Mode
		{
		    UTF8_PROCESS_SYMBOL = UTF8_XOR_NOMAC_SYMBOL;
		    if (extension.equals(bit_extension))	{ targetDestinPath = Paths.get(newTargetSourceFCPath.path.toString().substring(0, newTargetSourceFCPath.path.toString().lastIndexOf('.'))); }
		    else					{ targetDestinPath = newTargetSourceFCPath.path.resolveSibling(newTargetSourceFCPath.path.getFileName().toString() + bit_extension); }
		}
//		ui.log("newTargetSourceFCPath: " + newTargetSourceFCPath.path.toString() + "\r\n", true, true, true, false, false);
//		ui.log("targetDestinPath:      " + targetDestinPath.toAbsolutePath().toString() + "\r\n", true, true, true, false, false);
		
//		End of enxtension codeblock ===================================================================================================================================================================

//		At the start of the encryption process
		try { Files.deleteIfExists(targetDestinPath); } catch (IOException ex) { ui.log("Error: Files.deleteIfExists(targetDestinPath): " + ex.getMessage() + "\r\n", true, true, true, true, false); }

		// Prints printByte Header ones
		if ( print )
		{		    
		    printString = "\r\n";
		    printString += " -----------------------------------------------------------\r\n";
		    printString += "|       Input       |         Key       |      Output       |\r\n";
		    printString += "|-------------------|-------------------|-------------------|\r\n";
		    printString += "| bin      hx dec c | bin      hx dec c | bin      hx dec c |\r\n";
		    printString += "|-------------------|-------------------|-------------------|\r\n";
		}
//___________________________________________________________________________________________________________________________________________________________
//
//			Testing FinalCrypt Token
//			🔒   Encrypt
//			🔓   Decrypt	    (Key Authenticated)
//			🔓!  Decrypt Legacy  (Key can't be checked! No Token present in old format)
//			⛔   Decrypt Abort   (Key Failed)

		long readTargetSourceChannelPosition = 0;	long writeTargetDestChannelTransfered = 0;
		
		if (! disableMAC) // Be carefull: TRUE value is highly dangerous
		{
		    if (encryptmode)
		    {
			if ( newTargetSourceFCPath.isDecrypted) // Target has NO Token, Decrypted
			{
			    if (newTargetSourceFCPath.isEncryptable) // TargetSource is (Encryptable)
			    {				
				ui.log(UTF8_PROCESS_SYMBOL + " \"" + targetDestinPath.toAbsolutePath().toString() + "\" ", true, false, false, false, false);
				ui.log(UTF8_PROCESS_SYMBOL + " \"" + targetDestinPath.toAbsolutePath().toString() + "\" " + UTF8_PROCESS_SYMBOL, false, true, true, false, false);

				if ( ! dry )
				{
				    // Add MAC to targetDestinPath
				    ByteBuffer targetDestinMACBuffer = ByteBuffer.allocate((FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length() * 2)); targetDestinMACBuffer.clear();			
				    try (final SeekableByteChannel writeTargetDestinChannel = Files.newByteChannel(targetDestinPath, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC)))
				    {
					targetDestinMACBuffer = createTargetDestinMessageAuthenticationCode(keySourceFCPath.path);
					writeTargetDestChannelTransfered = writeTargetDestinChannel.write(targetDestinMACBuffer); targetDestinMACBuffer.flip();
					writeTargetDestinChannel.close();
					dstMessageDigest.update(targetDestinMACBuffer); // Build up checksum

					// wrteTargetDestinStat.addFileBytesProcessed(writeTargetDestChannelTransfered);
				    } catch (IOException ex) { ui.log("\r\nError: Add Token writeTargetDestinChannel Abort Encrypting: " + targetDestinPath.toString() + " " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }
				}
			    }
			    else // Decrypted but NOT Encryptable (should not be in the list anyway)
			    {
				ui.log(UTF8_UNENCRYPTABLE_SYMBOL + " \"" + newTargetSourceFCPath.toString() + "\" - Not Encryptable!\r\n", true, true, true, true, false);
				continue encryptTargetloop;
			    }
			}
		    }
		    else
		    {
			if (newTargetSourceFCPath.isEncrypted) // Target has MAC, Decrypt New Format
			{
			    if (newTargetSourceFCPath.isDecryptable) // TargetSource Has Authenticated MAC (Decryptable)
			    {
				ui.log(UTF8_PROCESS_SYMBOL + " \"" + targetDestinPath.toString() + "\" ", true, false, false, false, false);
				ui.log(UTF8_PROCESS_SYMBOL + " \"" + targetDestinPath.toString() + "\" " + UTF8_PROCESS_SYMBOL, false, true, true, false, false);
				
				ByteBuffer targetSourceBuffer = ByteBuffer.allocate(((FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length() * 2))); targetSourceBuffer.clear();
				try (final SeekableByteChannel readTargetSourceChannel = Files.newByteChannel(newTargetSourceFCPath.path, EnumSet.of(StandardOpenOption.READ)))
				{
				    // Fill up inputFileBuffer
				    readTargetSourceChannel.read(targetSourceBuffer); targetSourceBuffer.flip();
				    readTargetSourceChannel.close();
				    srcMessageDigest.update(targetSourceBuffer); // Build up checksum
				} catch (IOException ex) { ui.log("Error: readTargetSourceChannel = Files.newByteChannel(..) " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }

				readTargetSourceChannelPosition = (FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length() * 2); // Decrypt skipping MAC bytes at beginning
			    }
			    else
			    {
				ui.log(UTF8_UNDECRYPTABLE_SYMBOL + " \"" + newTargetSourceFCPath.toString() + "\" - Key Failed : " + keySourceFCPath.toString() + "\r\n", true, true, true, true, false);
				continue encryptTargetloop;
			    }
			}
		    }
		}
		else
		{
		    fileStatusLine =    UTF8_PROCESS_SYMBOL + " \"" + targetDestinPath.toAbsolutePath().toString() + "\" " + UTF8_PROCESS_SYMBOL;
		    ui.log(fileStatusLine, true, true, true, false, false);
		}
		
		
//___________________________________________________________________________________________________________________________________________________________
//
//			Encryptor I/O Block

		pwdPos = 0;

		ByteBuffer targetSourceBuffer = ByteBuffer.allocate(readTargetSourceBufferSize); targetSourceBuffer.clear();
		ByteBuffer keySourceBuffer = ByteBuffer.allocate(readTargetSourceBufferSize); keySourceBuffer.clear();
		ByteBuffer targetDestinBuffer = ByteBuffer.allocate(readTargetSourceBufferSize); targetDestinBuffer.clear();

		targetSourceEnded = false;
							    long    readTargetSourceChannelTransfered =  0;
		long readKeySourceChannelPosition = 0;	    long    readKeySourceChannelTransfered =  0;                
		long writeTargetDestChannelPosition = 0;	    writeTargetDestChannelTransfered =   0;
		long readTargetDestChannelPosition = 0;	    long    readTargetDestChannelTransfered =    0;
		long writeTargetSourceChannelPosition = 0;  long    writeTargetSourceChannelTransfered = 0;

		// Get and set the stats
//		    allDataStats.setFileBytesTotal(targetSourceSize);
		allDataStats.setFileBytesTotal(newTargetSourceFCPath.size);

		readTargetSourceStat.setFileBytesProcessed(0);	    readTargetSourceStat.setFileBytesTotal(newTargetSourceFCPath.size);
//                        readKeySourceStat.setFileBytesProcessed(0);      readKeySourceStat.setFileBytesTotal(filesize);
//                        wrteTargetDestinStat.setFileBytesProcessed(0);      wrteTargetDestinStat.setFileBytesTotal(filesize);
//                        readTargetDestinStat.setFileBytesProcessed(0);      readTargetDestinStat.setFileBytesTotal(filesize);
		wrteTargetSourceStat.setFileBytesProcessed(0);	    wrteTargetSourceStat.setFileBytesTotal(newTargetSourceFCPath.size);

		// Open and close files after every bufferrun. Interrupted file I/O works much faster than uninterrupted I/O encryption
		while (( ! targetSourceEnded ) && ( ! dry ))
		{
//                  Delete broken outputFile and keep original
//		    At the encryption stage of the process
		    if (stopPending)
		    {
			boolean deleted = false;
			try { deleted = Files.deleteIfExists(targetDestinPath); } catch (IOException ex) { ui.log("Error: Files.deleteIfExists(targetDestinPath): " + ex.getMessage() + "\r\n", true, true, true, true, false); }
			if ( deleted ) { ui.log(UTF8_STOP_SYMBOL + " " + UTF8_DELETE_SYMBOL + UTF8_FINISHED_SYMBOL + " ", false, true, true, false, false); } else { ui.log(UTF8_STOP_SYMBOL + " " + UTF8_DELETE_SYMBOL + " ", false, true, true, false, false); }
			targetSourceEnded = true;
			ui.log("\r\n", true, true, true, false, false);
			break encryptTargetloop;
		    }

		    //open targetSourcePath
		    readTargetSourceStat.setFileStartEpoch(); // allFilesStats.setFilesStartNanoTime();
		    try (final SeekableByteChannel readTargetSourceChannel = Files.newByteChannel(newTargetSourceFCPath.path, EnumSet.of(StandardOpenOption.READ)))
		    {
			// Fill up inputFileBuffer
			readTargetSourceChannel.position(readTargetSourceChannelPosition);
			readTargetSourceChannelTransfered = readTargetSourceChannel.read(targetSourceBuffer); targetSourceBuffer.flip(); readTargetSourceChannelPosition += readTargetSourceChannelTransfered;
			if (( readTargetSourceChannelTransfered == -1 ) || ( targetSourceBuffer.limit() < readTargetSourceBufferSize )) { targetSourceEnded = true; } // Buffer.limit = remainder from current position to end
			readTargetSourceChannel.close();
			srcMessageDigest.update(targetSourceBuffer); // Build up checksum
			    
			readTargetSourceStat.setFileEndEpoch(); readTargetSourceStat.clock();
			readTargetSourceStat.addFileBytesProcessed(readTargetSourceChannelTransfered / 2);
			allDataStats.addAllDataBytesProcessed("rd src", readTargetSourceChannelTransfered / 2);
		    } catch (IOException ex) { ui.log("Error: readTargetSourceChannel = Files.newByteChannel(..) " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }
//                            ui.log("readTargetSourceChannelTransfered: " + readTargetSourceChannelTransfered + " targetSourceBuffer.limit(): " + Integer.toString(targetSourceBuffer.limit()) + "\r\n");

		    if ( readTargetSourceChannelTransfered != -1 )
		    {
//                                readKeySourceStat.setFileStartEpoch();
			try (final SeekableByteChannel readKeySourceChannel = Files.newByteChannel(keySourceFCPath.path, EnumSet.of(StandardOpenOption.READ,StandardOpenOption.SYNC)))
			{
			    // Fill up keyFileBuffer
			    readKeySourceChannel.position(readKeySourceChannelPosition);
			    readKeySourceChannelTransfered = readKeySourceChannel.read(keySourceBuffer); readKeySourceChannelPosition += readKeySourceChannelTransfered;
			    if ( readKeySourceChannelTransfered < readKeySourceBufferSize ) { readKeySourceChannelPosition = 0; readKeySourceChannel.position(0); readKeySourceChannelTransfered += readKeySourceChannel.read(keySourceBuffer); readKeySourceChannelPosition += readKeySourceChannelTransfered;}
			    keySourceBuffer.flip();
			    readKeySourceChannel.close();
//				    readKeySourceStat.setFileEndEpoch(); readKeySourceStat.clock();
//                                    readKeySourceStat.addFileBytesProcessed(readKeySourceChannelTransfered);
			} catch (IOException ex) { ui.log("Error: readKeySourceChannel = Files.newByteChannel(..) " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }
//                                ui.log("readKeyFileChannelTransfered: " + readKeySourceChannelTransfered + " keySourceBuffer.limit(): " + Integer.toString(keySourceBuffer.limit()) + "\r\n");

			// Open outputFile for writing
//                                wrteTargetDestinStat.setFileStartEpoch();
			try (final SeekableByteChannel writeTargetDestinChannel = Files.newByteChannel(targetDestinPath, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC)))
			{
			    // Encrypt inputBuffer and fill up outputBuffer
			    targetDestinBuffer = encryptBuffer(targetSourceBuffer, keySourceBuffer, true); // last boolean = PrintEnabled
			    writeTargetDestChannelTransfered = writeTargetDestinChannel.write(targetDestinBuffer); targetDestinBuffer.flip(); writeTargetDestChannelPosition += writeTargetDestChannelTransfered;
			    if (txt) { logByteBuffer("DB", targetSourceBuffer); logByteBuffer("CB", keySourceBuffer); logByteBuffer("OB", targetDestinBuffer); }
			    writeTargetDestinChannel.close();
			    dstMessageDigest.update(targetDestinBuffer); // Build up checksum
//				    wrteTargetDestinStat.setFileEndEpoch(); wrteTargetDestinStat.clock();
//                                    wrteTargetDestinStat.addFileBytesProcessed(writeTargetDestChannelTransfered);
			} catch (IOException ex) { ui.log("Error: writeTargetDestinChannel = Files.newByteChannel(..) " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }
//                            ui.log("writeTargetDestChannelTransfered: " + writeTargetDestChannelTransfered + " targetDestinBuffer.limit(): " + Integer.toString(targetDestinBuffer.limit()) + "\r\n");
		    }
		    targetDestinBuffer.clear(); targetSourceBuffer.clear(); keySourceBuffer.clear();		    
		} // targetSourceEnded

//    ==================================================================================================================================================================
//                      Copy inputFilePath attributes to outputFilePath

/*
“basic:creationTime”	FileTime	The exact time when the file was created.
“basic:fileKey”	Object	An object that uniquely identifies a file or null if a file key is not available.
“basic:isDirectory”	Boolean	Returns true if the file is a directory.
“basic:isRegularFile”	Boolean	Returns true if a file is not a directory.
“basic:isSymbolicLink”	Boolean	Returns true if the file is considered to be a symbolic link.
“basic:isOther”	Boolean	
“basic:lastAccessTime”	FileTime	The last time when the file was accesed.
“basic:lastModifiedTime”	FileTime	The time when the file was last modified.
“basic:size”	Long	The file size.    

“dos:archive”	Boolean	Return true if a file is archive or not.
“dos:hidden”	Boolean	Returns true if the file/folder is hidden.
“dos:readonly”	Boolean	Returns true if the file/folder is read-only.
“dos:system”	Boolean	Returns true if the file/folder is system file.

“posix:permissions”	Set<PosixFilePermission>	The file permissions.
“posix:group”	GroupPrincipal	Used to determine access rights to objects in a file system

“acl:acl”	List<AclEntry>
“acl:owner”	UserPrincipal
*/

		if ( ! dry)
		{
		    attributeViewloop: for (String view:newTargetSourceFCPath.path.getFileSystem().supportedFileAttributeViews()) // acl basic owner user dos
		    {
//                            ui.println(view);
			if ( view.toLowerCase().equals("basic") )
			{
			    try
			    {
				BasicFileAttributes basicAttributes = null; basicAttributes = Files.readAttributes(newTargetSourceFCPath.path, BasicFileAttributes.class);
				try
				{
				    Files.setAttribute(targetDestinPath, "basic:creationTime",        basicAttributes.creationTime());
				    Files.setAttribute(targetDestinPath, "basic:lastModifiedTime",    basicAttributes.lastModifiedTime());
				    Files.setAttribute(targetDestinPath, "basic:lastAccessTime",      basicAttributes.lastAccessTime());
				}
				catch (IOException ex) { ui.log("Error: Set Basic Attributes: " + ex.getMessage() + "\r\n", false, false, true, true, false); }
			    }   catch (IOException ex) { ui.log("Error: basicAttributes = Files.readAttributes(..): " + ex.getMessage() + "\r\n", false, false, true, true, false); }
			}
			else if ( view.toLowerCase().equals("dos") )
			{
			    try
			    {
				DosFileAttributes msdosAttributes = null; msdosAttributes = Files.readAttributes(newTargetSourceFCPath.path, DosFileAttributes.class);
				try
				{
				    Files.setAttribute(targetDestinPath, "basic:lastModifiedTime",    msdosAttributes.lastModifiedTime());
				    Files.setAttribute(targetDestinPath, "dos:hidden",                msdosAttributes.isHidden());
				    Files.setAttribute(targetDestinPath, "dos:system",                msdosAttributes.isSystem());
				    Files.setAttribute(targetDestinPath, "dos:readonly",              msdosAttributes.isReadOnly());
				    Files.setAttribute(targetDestinPath, "dos:archive",               msdosAttributes.isArchive());
				}
				catch (IOException ex) { ui.log("Error: Set DOS Attributes: " + ex.getMessage() + "\r\n", false, false, true, true, false); }
			    }   catch (IOException ex) { ui.log("Error: msdosAttributes = Files.readAttributes(..): " + ex.getMessage() + "\r\n", false, false, true, true, false); }
			}
			else if ( view.toLowerCase().equals("posix") )
			{
			    PosixFileAttributes posixAttributes = null;
			    try
			    {
				posixAttributes = Files.readAttributes(newTargetSourceFCPath.path, PosixFileAttributes.class);
				try
				{
				    Files.setAttribute(targetDestinPath, "posix:owner",               posixAttributes.owner());
				    Files.setAttribute(targetDestinPath, "posix:group",               posixAttributes.group());
				    Files.setPosixFilePermissions(targetDestinPath,                   posixAttributes.permissions());
				    Files.setLastModifiedTime(targetDestinPath,                       posixAttributes.lastModifiedTime());
				}
				catch (IOException ex) { ui.log("Error: Set POSIX Attributes: " + ex.getMessage() + "\r\n", false, false, true, true, false); }
			    }   catch (IOException ex) { ui.log("Error: posixAttributes = Files.readAttributes(..): " + ex.getMessage() + "\r\n", false, false, true, true, false); }
			}
		    } // End attributeViewloop // End attributeViewloop
		} // End ! dry

//    ==================================================================================================================================================================

//                      Counting encrypting and shredding for the average throughtput performance

//                      Shredding process
		
		ui.log(UTF8_CLONE_SYMBOL + " \"" + newTargetSourceFCPath.path.toAbsolutePath() + "\" ", true, false, false, false, false); // 🌊🗑
		ui.log(UTF8_FINISHED_SYMBOL + " " + UTF8_CLONE_SYMBOL, false, true, true, false, false);

		long targetDestinSize = 0; double targetDiffFactor = 1;

		if ( ! dry)
		{
//				     isValidFile(UI ui, String caller,    Path path, boolean isKey, boolean device, long minSize, boolean symlink, boolean writable, boolean report)
		    if (Validate.isValidFile(   ui,            "", targetDestinPath,		false,		false,            1,           false,            false,	    true))
		    { try { targetDestinSize = Files.size(targetDestinPath); targetDiffFactor = newTargetSourceFCPath.size / targetDestinSize;} catch (IOException ex) { ui.log("Error: Files.size(targetDestinPath); " + ex.getMessage() + "\r\n", true, true, true, true, false); } } else 

		    readTargetSourceChannelPosition = 0;    readTargetSourceChannelTransfered = 0;
		    readKeySourceChannelPosition = 0;	    readKeySourceChannelTransfered = 0;

		    writeTargetDestChannelPosition = 0;

		    targetSourceBuffer = ByteBuffer.allocate(readTargetSourceBufferSize); targetSourceBuffer.clear();
		    keySourceBuffer = ByteBuffer.allocate(readKeySourceBufferSize); keySourceBuffer.clear();
		    targetDestinBuffer = ByteBuffer.allocate(wrteTargetDestinBufferSize); targetDestinBuffer.clear();

		    boolean targetDestinEnded = false;

		    shredloop: while ( ! targetDestinEnded )
		    {
			while (pausing)     { try { Thread.sleep(100); } catch (InterruptedException ex) {  } }
			
//			Delete broken outputFile and keep original
//			At the shredding stage of the process
			if (stopPending)
			{
			    boolean deleted = false;
			    try { deleted = Files.deleteIfExists(newTargetSourceFCPath.path); } catch (IOException ex) { ui.log("Error: Files.deleteIfExists(" + newTargetSourceFCPath.path.toString() + "): " + ex.getMessage() + "\r\n", true, true, true, true, false); }
			    if ( deleted ) { ui.log(UTF8_STOP_SYMBOL + " " + UTF8_DELETE_SYMBOL + UTF8_FINISHED_SYMBOL + " ", false, true, true, false, false); } else { ui.log(UTF8_STOP_SYMBOL + " " + UTF8_DELETE_SYMBOL + " ", false, true, true, false, false); }
			    targetSourceEnded = true;
//			    ui.log("\r\n", true, true, true, false, false);
			    targetDestinEnded = true;

			    byte[] srcHashBytes = srcMessageDigest.digest();
			    String srcHashString = getHexString(srcHashBytes,2); // print checksum

			    byte[] dstHashBytes = dstMessageDigest.digest();
			    String dstHashString = getHexString(dstHashBytes,2); // print checksum

			    fileStatusLine = allDataStats.getAllDataBytesProgressPercentage();
			    ui.log(HASH_ALGORITHM_NAME + ": \"" + srcHashString + "\"->\"" + dstHashString + "\" " + fileStatusLine + "\r\n", true, true, true, false, false);
			    
			    break encryptTargetloop;
			}

//			if (stopPending)    { targetDestinEnded = true; break shredloop; }

			//read outputFile
//                            readTargetDestinStat.setFileStartEpoch();
			try (final SeekableByteChannel readTargetDestinChannel = Files.newByteChannel(targetDestinPath, EnumSet.of(StandardOpenOption.READ)))
			{
			    readTargetDestinChannel.position(readTargetDestChannelPosition);
			    readTargetDestChannelTransfered = readTargetDestinChannel.read(targetDestinBuffer); targetDestinBuffer.flip(); readTargetDestChannelPosition += readTargetDestChannelTransfered;
			    if (( readTargetDestChannelTransfered < 1 )) { targetDestinEnded = true; }
			    readTargetDestinChannel.close();
			} catch (IOException ex) { ui.log("\r\nError: readTargetDestinChannel = Files.newByteChannel(..) " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }
//                            ui.log("readTargetDestChannelTransfered: " + readTargetDestChannelTransfered + " targetDestinBuffer.limit(): " + Integer.toString( targetDestinBuffer.limit()) + "\r\n");

			//shred inputFile
//                            if ( readTargetDestChannelTransfered < 1 )
			if ( targetDestinBuffer.limit() > 0 )
			{
			    wrteTargetSourceStat.setFileStartEpoch();
			    try (final SeekableByteChannel writeTargetSourceChannel = Files.newByteChannel(newTargetSourceFCPath.path, EnumSet.of(StandardOpenOption.WRITE,StandardOpenOption.SYNC)))
			    {
				// Fill up inputFileBuffer
				writeTargetSourceChannel.position(writeTargetSourceChannelPosition);
				writeTargetSourceChannelTransfered = writeTargetSourceChannel.write(targetDestinBuffer); targetSourceBuffer.flip(); writeTargetSourceChannelPosition += writeTargetSourceChannelTransfered;
				if (( writeTargetSourceChannelTransfered < 1 )) { targetSourceEnded = true; }
				writeTargetSourceChannel.close();
				wrteTargetSourceStat.setFileEndEpoch(); wrteTargetSourceStat.clock();
				wrteTargetSourceStat.addFileBytesProcessed(writeTargetSourceChannelTransfered / 2);
				allDataStats.addAllDataBytesProcessed("wr src", writeTargetSourceChannelTransfered / 2);
//				    if ( targetDiffFactor < 1 )
//				    { allDataStats.addAllDataBytesProcessed("wr src", writeTargetSourceChannelTransfered * Math.abs((long)targetDiffFactor)); } else
//				    { allDataStats.addAllDataBytesProcessed("wr src", writeTargetSourceChannelTransfered / Math.abs((long)targetDiffFactor)); }

			    } catch (IOException ex) { ui.log("\r\nError: writeTargetSourceChannel = Files.newByteChannel(..) " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }
//                                ui.log("writeTargetSourceChannelTransfered: " + writeTargetSourceChannelTransfered + " targetDestinBuffer.limit(): " + Integer.toString(targetDestinBuffer.limit()) + "\r\n");
			}
			targetDestinBuffer.clear(); targetSourceBuffer.clear(); keySourceBuffer.clear();
		    }

		    ui.log(UTF8_CLONE_SYMBOL + " \"" + newTargetSourceFCPath.path.toAbsolutePath() + "\" ", true, false, false, false, false); // 🌊🗑
		    ui.log(UTF8_FINISHED_SYMBOL + " ", false, true, true, false, false);

//                  FILE STATUS 
		    if (verbose)
		    {
//			fileStatusLine += "- Crypt: rd(" +  readTargetSourceStat.getFileBytesThroughPut() + ") -> ";
			fileStatusLine = "- Crypt: rd(" +  readTargetSourceStat.getFileBytesThroughPut() + ") -> ";
			
//			    fileStatusLine += "rd(" +           readKeySourceStat.getFileBytesThroughPut() + ") -> ";
//			    fileStatusLine += "wr(" +           wrteTargetDestinStat.getFileBytesThroughPut() + ") ";
//			    fileStatusLine += "- Shred: rd(" +  readTargetDestinStat.getFileBytesThroughPut() + ")";

//			fileStatusLine += "wr(" +           wrteTargetSourceStat.getFileBytesThroughPut() + ") ";
			fileStatusLine = "wr(" +           wrteTargetSourceStat.getFileBytesThroughPut() + ") ";
		    }
		} // End ! dry


//		if ( print ) { ui.log(" ----------------------------------------------------------------------\r\n"); } // Tail after printheader


//              Delete the original
		if ( ! dry)
		{
		    if
		    (
			( newTargetSourceFCPath.size != 0 ) && ( targetDestinSize != 0 ) &&
			( Math.abs(newTargetSourceFCPath.size - targetDestinSize)  == (FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length()) * 2 ) ||
			( newTargetSourceFCPath.size == targetDestinSize)
		    )
		    {
//			After the shredding stage of the process
			boolean deleted = false;
			try { deleted = Files.deleteIfExists(newTargetSourceFCPath.path); } catch (IOException ex)    { ui.log("Error: Files.deleteIfExists(" + newTargetSourceFCPath.path.toString() + "): " + ex.getMessage() + "\r\n", true, true, true, true, false); continue encryptTargetloop; }
			if ( deleted ) { ui.log(UTF8_DELETE_SYMBOL + UTF8_FINISHED_SYMBOL + " ", false, true, true, false, false); } else { ui.log(UTF8_DELETE_SYMBOL + " ", false, true, true, false, false); }
		    }
		}

		byte[] srcHashBytes = srcMessageDigest.digest();
		String srcHashString = getHexString(srcHashBytes,2); // print checksum

		byte[] dstHashBytes = dstMessageDigest.digest();
		String dstHashString = getHexString(dstHashBytes,2); // print checksum
		
		fileStatusLine = allDataStats.getAllDataBytesProgressPercentage();
		if (! dry)
		{
		    ui.log(HASH_ALGORITHM_NAME + ": \"" + srcHashString + "\"->\"" + dstHashString + "\" " + fileStatusLine + "\r\n", true, true, true, false, false);		    
		}
		else
		{
		    ui.log(fileStatusLine + "\r\n", true, true, true, false, false);		    
		}

		if ( print )
		{
		    printString += " -----------------------------------------------------------\r\n"; // Footer
		    ui.log(printString + "\r\n", true, true, true, false, false);
		}

		allDataStats.addFilesProcessed(1);
	    } // else { ui.error(targetSourcePath.toAbsolutePath() + " ignoring:   " + keySourcePath.toAbsolutePath() + " (is key!)\r\n"); }
	    
	    
	    
//	    ===================================================================================================================================================
	    


//					     getFCPath(UI ui, String caller,	    Path path, boolean isKey,		 Path keyPath, boolean report)
	    newTargetSourceFCPath = Validate.getFCPath(   ui,            "", targetDestinPath,		  false, keySourceFCPath.path,	 verbose);
	    if ( newTargetSourceFCPath.isEncrypted ) { newTargetSourceFCPath.isNewEncrypted = true; } else { newTargetSourceFCPath.isNewDecrypted = true; }
	    targetSourceFCPathList.updateStat(oldTargetSourceFCPath, newTargetSourceFCPath); ui.fileProgress();
        } // End Encrypt Files Loop // End Encrypt Files Loop // End Encrypt Files Loop // End Encrypt Files Loop
	
	bytesPerMilliSecond = 0.0;
        allDataStats.setAllDataEndNanoTime(); allDataStats.clock();
        if ( stopPending ) { ui.log("\r\n", true, false, false, false, false); stopPending = false;  } // It breaks in the middle of encrypting, so the encryption summery needs to begin on a new line

//      Print the stats
        ui.log(allDataStats.getEndSummary(modeDesc), true, true, true, false, false);

        updateProgressTaskTimer.cancel(); updateProgressTaskTimer.purge();
//        updateProgressTimeline.stop();
	processRunning = false;
	ui.processFinished(filteredTargetSourceFCPathList, open);
    }
    
    synchronized public static String getHexString(byte[] bytes, int digits) { String returnString = ""; for (byte mybyte:bytes) { returnString += getHexString(mybyte, digits); } return returnString; }
    synchronized public static String getHexString(byte value, int digits) { return String.format("%0" + Integer.toString(digits) + "X", (value & 0xFF)).replaceAll("[^A-Za-z0-9]",""); }

    public static ByteBuffer encryptBuffer(ByteBuffer targetSourceBuffer, ByteBuffer keySourceBuffer, boolean printEnabled)
    {
	long startTime = System.nanoTime();
        ByteBuffer targetDestinBuffer = ByteBuffer.allocate(keySourceBuffer.capacity()); targetDestinBuffer.clear();
	
        while (pausing)     { try { Thread.sleep(100); } catch (InterruptedException ex) {  } }
        byte targetDestinByte;
	for (int targetSourceBufferCount = 0; targetSourceBufferCount < targetSourceBuffer.limit(); targetSourceBufferCount++)
        {
	    byte targetSourceByte = targetSourceBuffer.get(targetSourceBufferCount);
	    byte keySourceByte = keySourceBuffer.get(targetSourceBufferCount);
	    targetDestinByte = encryptByte(targetSourceByte, keySourceByte); targetDestinBuffer.put(targetDestinByte);
	    if ((printEnabled) && ( print )) { printString += getByteString(targetSourceByte, keySourceByte, targetDestinByte); }
	}
        targetDestinBuffer.flip();
	
	long endTime = System.nanoTime();
	double secondsFactor = (1000000 / (endTime - startTime));
	bytesPerMilliSecond = (targetDestinBuffer.limit() * secondsFactor);
	
	return targetDestinBuffer;
    }
    
    public static byte encryptByte(final byte targetSourceByte, byte keySourceByte)
    {
	byte returnByte; // Final result to return
//	byte keyXORByte;
	
        if (keySourceByte == 0) { keySourceByte = (byte)(~keySourceByte & 0xFF); } // Inverting / negate key 0 bytes (none encryption not allowed)
	
	if ( pwd.length() == 0 ) // No extra password encryption
	{
	    returnByte = (byte)(targetSourceByte ^ keySourceByte);
	}
	else
	{
	    byte keyXORByte = (byte)(targetSourceByte ^ keySourceByte);
	    returnByte = (byte)(keyXORByte ^ (byte)pwd.charAt(pwdPos)); pwdPos++;
	    if ( pwdPos == pwd.length() ) { pwdPos = 0; }
	}
	
	return	returnByte;
    }

    public static byte encryptByteFastXOR(final byte targetSourceByte, byte keySourceByte)
    {
        byte targetDestinEncryptedByte;

        int targetDestinIgnoreBits = 0;
        int targetDestinKeyBits = 0;
        int targetDestinMergedBits = 0; // Merged Ignored & Negated bits)
	        
        if (keySourceByte == 0) { keySourceByte = (byte)(~keySourceByte & 0xFF); } // Inverting / negate key 0 bytes (none encryption not allowed)
//
//	The following 4 line are the encrypting heart of FinalCrypt.
//												    _______________________  _______________________   ________  ___________________________________________
//												   /------- LINE 1 --------\/------ LINE 2 ---------\ / LINE 3 \/            Encrypt            Decrypt     \
        targetDestinIgnoreBits =	targetSourceByte & ~keySourceByte;		// LINE 1 |             00000011   | ~00000011 = 11111100   | 00000010 | Data byte: 00000011 = 3   ╭─> 00000110 = 6 |
        targetDestinKeyBits =	~targetSourceByte & keySourceByte;			// LINE 2 | ~00000101 = 11111010 & |             00000101 & | 00000100 | Ciph byte: 00000101 = 5   │   00000101 = 5 |
        targetDestinMergedBits =	targetDestinIgnoreBits + targetDestinKeyBits;	// LINE 3 |	        00000010   |             00000100   | 00000110 | Encr byte: 00000110 = 6 ─╯    00000011 = 3 |
        targetDestinEncryptedByte =	(byte)(targetDestinMergedBits & 0xFF);		// Make sure only 8 bits of the 16 bit integer gets set in the byte casted encrypted byte

        if ( bin )      { logByteBinary(targetSourceByte, keySourceByte, targetDestinEncryptedByte, targetDestinIgnoreBits, targetDestinKeyBits, targetDestinMergedBits); }
        if ( dec )      { logByteDecimal(targetSourceByte, keySourceByte, targetDestinEncryptedByte, targetDestinIgnoreBits, targetDestinKeyBits, targetDestinMergedBits); }
        if ( hex )      { logByteHexaDecimal(targetSourceByte, keySourceByte, targetDestinEncryptedByte, targetDestinIgnoreBits, targetDestinKeyBits, targetDestinMergedBits); }
        if ( chr )      { logByteChar(targetSourceByte, keySourceByte, targetDestinEncryptedByte, targetDestinIgnoreBits, targetDestinKeyBits, targetDestinMergedBits); }

        return targetDestinEncryptedByte;
    }

    private ByteBuffer createTargetDestinMessageAuthenticationCode(Path keySourcePath) // Tested
    {
        ByteBuffer plainTextMACBuffer = ByteBuffer.allocate(FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length()); plainTextMACBuffer.clear();
        ByteBuffer keyBitMACBuffer = ByteBuffer.allocate(FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length()); keyBitMACBuffer.clear();
        ByteBuffer encryptedMACBuffer = ByteBuffer.allocate(FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length()); encryptedMACBuffer.clear();

	ByteBuffer targetDstMACBuffer = ByteBuffer.allocate(FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length() * 2); targetDstMACBuffer.clear();
	long readKeySourceChannelTransfered = 0;                

	// Create plaint text Buffer
	plainTextMACBuffer.put(FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.getBytes());
	
	// Create Key Buffer
	try (final SeekableByteChannel readKeySourceChannel = Files.newByteChannel(keySourcePath, EnumSet.of(StandardOpenOption.READ)))
	{
//	    readKeySourceChannel.position(readKeySourceChannelPosition);
	    readKeySourceChannelTransfered = readKeySourceChannel.read(keyBitMACBuffer);
	    keyBitMACBuffer.flip(); readKeySourceChannel.close();
	} catch (IOException ex) { ui.log("Error: getTargetDestinMAC: readKeySourceChannel " + ex.getMessage() + "\r\n", true, true, true, true, false); }
	
	// Create Encrypted Token Buffer
	encryptedMACBuffer = encryptBuffer(plainTextMACBuffer, keyBitMACBuffer, false);
	
	// Create Target Destin Token Buffer
	byte[] messageAuthenticationCodeArray = new byte[(FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length() * 2)];
	for (int x = 0; x < plainTextMACBuffer.capacity(); x++) { messageAuthenticationCodeArray[x] = plainTextMACBuffer.array()[x]; }
	for (int x = 0; x < encryptedMACBuffer.capacity(); x++) { messageAuthenticationCodeArray[(FINALCRYPT_PLAIN_TEXT_MESSAGE_AUTHENTICATION_CODE.length() + x)] = encryptedMACBuffer.array()[x]; }
	targetDstMACBuffer.put(messageAuthenticationCodeArray); targetDstMACBuffer.flip();
	
	pwdPos = 0;
	
	return targetDstMACBuffer;
    }
    
//  Recursive Deletion of PathList
    public void deleteSelection(ArrayList<Path> targetSourcePathList, boolean delete, boolean returnpathlist, String pattern, boolean negatePattern)
    {
        EnumSet opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS); //follow links
//							  MySimpleFileVisitor(UI ui, boolean verbose, boolean delete, long minSize, boolean symlink, boolean writable, boolean returnpathlist, ArrayList<FCPath>(),    String pattern, boolean negatePattern)
//							  MySimpleFCFileVisitor(UI ui, boolean verbose, boolean delete, boolean symlink, boolean setFCPathlist,    Path keyPath, ArrayList<FCPath> targetFCPathList, String pattern, boolean negatePattern)
        MySimpleFCFileVisitor mySimpleFCFileVisitor = new MySimpleFCFileVisitor(   ui,	       verbose,         delete,         symlink,		 false,               null,            new FCPathList(),        pattern,         negatePattern);
        for (Path path:targetSourcePathList)
        {
            try{Files.walkFileTree(path, opts, Integer.MAX_VALUE, mySimpleFCFileVisitor);} catch(IOException e){System.err.println(e);}
        }
    }
    

    private static String getBinaryString(Byte myByte) { return String.format("%8s", Integer.toBinaryString(myByte & 0xFF)).replace(' ', '0'); }
    private static String getDecString(Byte myByte) { return String.format("%3d", (myByte & 0xFF)).replace(" ", "0"); }
    private static String getHexString(Byte myByte, String digits) { return String.format("%0" + digits + "X", (myByte & 0xFF)); }
    private static String getChar(Byte myByte) { return String.format("%1s", (char) (myByte & 0xFF)).replaceAll("\\p{C}", "?"); }  //  (myByte & 0xFF); }
    
    public boolean getPausing()             { return pausing; }
    public boolean getStopPending()         { return stopPending; }
    public void setPausing(boolean val)     { pausing = val; }
    public void setStopPending(boolean val) { stopPending = val; }
    
    public static void setPwd(String pwdParam)	    { pwd = pwdParam; }
    public static void resetPwdPos()		    { pwdPos = 0; }

    private static void logByteBuffer(String preFix, ByteBuffer byteBuffer)
    {
        System.out.println(preFix + "C: ");
        System.out.println(" " + preFix + "Z: " + byteBuffer.limit() + "\r\n");
    }

//    private static void logByte(byte dataByte, byte keyByte, byte outputByte)
    private static String getByteString(byte dataByte, byte keyByte, byte outputByte)
    {
        String datbin = getBinaryString(dataByte);
        String dathex = getHexString(dataByte, "2");
        String datdec = getDecString(dataByte);
        String datchr = getChar(dataByte);
        
        String cphbin = getBinaryString(keyByte);
        String cphhex = getHexString(keyByte, "2");
        String cphdec = getDecString(keyByte);
        String cphchr = getChar(keyByte);
        
        String outbin = getBinaryString(outputByte);
        String outhex = getHexString(outputByte, "2");
        String outdec = getDecString(outputByte);
        String outchr = getChar(outputByte);
        
//	System.out.print(datbin + " " +  dathex + " " + datdec + " " + datchr + " | ");
//        System.out.print(cphbin + " " +  cphhex + " " + cphdec + " " + cphchr + " | ");
//        System.out.print(outbin + " " +  outhex + " " + outdec + " " + outchr + " |");

	String returnString = "| ";
	returnString += datbin + " " +  dathex + " " + datdec + " " + datchr + " | ";
	returnString += cphbin + " " +  cphhex + " " + cphdec + " " + cphchr + " | ";
	returnString += outbin + " " +  outhex + " " + outdec + " " + outchr + " | \r\n";

	return returnString;
    }
    
    private static void logByteBinary(byte inputByte, byte keyByte, byte outputByte, int dum, int dnm, int dbm)
    {
        System.out.println("\r\n");
        System.out.println("Input  = " + getBinaryString(inputByte) + "\r\n");
        System.out.println("Key = " + getBinaryString(keyByte) + "\r\n");
        System.out.println("Output = " + getBinaryString(outputByte) + "\r\n");
        System.out.println("\r\n");
        System.out.println("DUM  = " + getBinaryString((byte)inputByte) + " & " + getBinaryString((byte)~keyByte) + " = " + getBinaryString((byte)dum) + "\r\n");
        System.out.println("DNM  = " + getBinaryString((byte)~inputByte) + " & " + getBinaryString((byte)keyByte) + " = " + getBinaryString((byte)dnm) + "\r\n");
        System.out.println("DBM  = " + getBinaryString((byte)dum) + " & " + getBinaryString((byte)dnm) + " = " + getBinaryString((byte)dbm) + "\r\n");
    }
    
    private static void logByteDecimal(byte dataByte, byte keyByte, byte outputByte, int dum, int dnm, int dbm)
    {
        System.out.println("\r\n");
        System.out.println("Input  = " + getDecString(dataByte) + "\r\n");
        System.out.println("Key = " + getDecString(keyByte) + "\r\n");
        System.out.println("Output = " + getDecString(outputByte) + "\r\n");
        System.out.println("\r\n");
        System.out.println("DUM  = " + getDecString((byte)dataByte) + " & " + getDecString((byte)~keyByte) + " = " + getDecString((byte)dum) + "\r\n");
        System.out.println("DNM  = " + getDecString((byte)~dataByte) + " & " + getDecString((byte)keyByte) + " = " + getDecString((byte)dnm) + "\r\n");
        System.out.println("DBM  = " + getDecString((byte)dum) + " & " + getDecString((byte)dnm) + " = " + getDecString((byte)dbm) + "\r\n");
    }
    
    private static void logByteHexaDecimal(byte dataByte, byte keyByte, byte outputByte, int dum, int dnm, int dbm)
    {
        System.out.println("\r\n");
        System.out.println("Input  = " + getHexString(dataByte,"2") + "\r\n");
        System.out.println("Key = " + getHexString(keyByte,"2") + "\r\n");
        System.out.println("Output = " + getHexString(outputByte,"2") + "\r\n");
        System.out.println("\r\n");
        System.out.println("DUM  = " + getHexString((byte)dataByte,"2") + " & " + getHexString((byte)~keyByte,"2") + " = " + getHexString((byte)dum,"2") + "\r\n");
        System.out.println("DNM  = " + getHexString((byte)~dataByte,"2") + " & " + getHexString((byte)keyByte,"2") + " = " + getHexString((byte)dnm,"2") + "\r\n");
        System.out.println("DBM  = " + getHexString((byte)dum,"2") + " & " + getHexString((byte)dnm,"2") + " = " + getHexString((byte)dbm,"2") + "\r\n");
    }
    
    private static void logByteChar(byte dataByte, byte keyByte, byte outputByte, int dum, int dnm, int dbm)
    {
        System.out.println("\r\n");
        System.out.println("Input  = " + getChar(dataByte) + "\r\n");
        System.out.println("Key = " + getChar(keyByte) + "\r\n");
        System.out.println("Output = " + getChar(outputByte) + "\r\n");
        System.out.println("\r\n");
        System.out.println("DUM  = " + getChar((byte)dataByte) + " & " + getChar((byte)~keyByte) + " = " + getChar((byte)dum) + "\r\n");
        System.out.println("DNM  = " + getChar((byte)~dataByte) + " & " + getChar((byte)keyByte) + " = " + getChar((byte)dnm) + "\r\n");
        System.out.println("DBM  = " + getChar((byte)dum) + " & " + getChar((byte)dnm) + " = " + getChar((byte)dbm) + "\r\n");
    }
    
    public ArrayList<Path> getPathList(File[] files)
    {
        // Converts from File[] to ArraayList<Path>
        ArrayList<Path> pathList = new ArrayList<>(); for (File file:files) { pathList.add(file.toPath()); }
        return pathList;
    }
    
//    public Stats getStats()                                 { return stats; }

//  Class Extends Thread
    @Override
    @SuppressWarnings("empty-statement")
    public void run()
    {
    }
}
