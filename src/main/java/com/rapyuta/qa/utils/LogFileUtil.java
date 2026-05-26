package com.rapyuta.qa.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LogFileUtil {
	private static final Logger log = Logger.getLogger(LogFileUtil.class.getName());
	public static String writeToFile(String fileName, String content) {
	    try {
	    	fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
	    	String dir = System.getProperty("user.dir") + "/reports/networkLogs/";
            File folder = new File(dir);
            if (!folder.exists() && !folder.mkdirs()) {
                System.err.println("Failed to create directory: " + dir);
                log.severe("Failed to create directory: " + dir);
                return null;
            }

            File file = new File(folder, fileName);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(content != null ? content : "No data captured");
            }
            log.info("Network log saved successfully: " + file.getAbsolutePath());
            return file.getAbsolutePath(); // Return full path for report linking
        } catch (IOException e) {
        	log.log(Level.SEVERE, "Error writing to log file: " + fileName, e);
            return null;
        }
	}

    }
    
