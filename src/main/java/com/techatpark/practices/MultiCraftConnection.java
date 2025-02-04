package com.techatpark.practices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MultiCraftConnection {

    public static String BASE_URL = "https://multicraft.ca";

    private final String cokkie;

    public MultiCraftConnection(String cokkie) {
        this.cokkie = cokkie;
    }

    public String getHTML(final String url) {
        // Define the cURL command (Example: Fetching Google's homepage)
        String[] command = {
            "curl" , BASE_URL + url
            ,"--compressed"
            ,"-H", "User-Agent: Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:127.0) Gecko/20100101 Firefox/127.0" 
            ,"-H", "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8" 
            ,"-H", "Accept-Language: en-US,en;q=0.5" 
            ,"-H", "Accept-Encoding: gzip, deflate, br, zstd" 
            ,"-H", "Connection: keep-alive" 
            ,"-H", "Cookie: " + cokkie 
            ,"-H", "Upgrade-Insecure-Requests: 1" 
            ,"-H", "Sec-Fetch-Dest: document" 
            ,"-H", "Sec-Fetch-Mode: navigate" 
            ,"-H", "Sec-Fetch-Site: none" 
            ,"-H", "Sec-Fetch-User: ?1" 
            ,"-H", "Priority: u=1"
        };

        StringBuilder builder = new StringBuilder();

        try {
            // Start the process
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // Merge stdout and stderr
            
            Process process = processBuilder.start();

            boolean startAppending = false ;

            // Read the output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {

                    if(startAppending) {
                        builder.append(line);
                    } else {
                        if(line.trim().equals("<!DOCTYPE html>")) {
                            startAppending = true;
                        }
                    }
                    
                }
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();


            

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
