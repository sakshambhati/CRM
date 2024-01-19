package com.crm.serviceImpl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.crm.config.AWSClientConfigService;
import com.crm.service.TessaractService;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TessaractServiceImpl implements TessaractService {

    @Autowired
    AWSClientConfigService awsClientConfigService;
    
    public String parseIMG(String bucket, String key, String token) throws IOException {

        AmazonS3 awsClient = awsClientConfigService.awsClientConfiguration(token);
        S3Object object = awsClient.getObject(bucket, key);
        InputStream is = object.getObjectContent();

        if(object==null) return null;
//        ByteArrayResource resource = new ByteArrayResource(bytes);
//        InputStream targetStream = new ByteArrayInputStream(data);
        Tesseract tesseract = new Tesseract();
        String str = "";
        tesseract.setDatapath("./Tess4J/tessdata");
//
        try {
            BufferedImage imageTo = ImageIO.read(is);
            str=tesseract.doOCR(imageTo);
        } catch (TesseractException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
//        String text = str.replaceAll("\\r|\\n|", " ");

        String text = str.replaceAll("\\s+", " ");
//        Pattern r = Pattern.compile("(?s) " + "PO" + "[^\\n]+");
//
//        Matcher m = r.matcher(text.toLowerCase());
//
//        HashMap<String, Long> ans = new HashMap<>();
//        while (m.find()) {
//            ans.put(m.group().replaceAll(text, "<b>" + "PO" + "</b>"), 1L);
//        }

//        text=" "+text.replaceAll("\\s+", " ");
//        return text;
        String searchText = "PO # ";
        int firstIndex = text.indexOf(searchText);

        // Check if searchText is found
        if (firstIndex != -1) {
            // Find the index of the second space (" ") after the first occurrence
            int secondSpaceIndex = text.indexOf(" ", firstIndex + searchText.length());

            // Check if the second space is found
            if (secondSpaceIndex != -1) {
                // Extract the substring between the first occurrence and the second space
                return text.substring(firstIndex + searchText.length(), secondSpaceIndex).trim();
            }
        }

        // Return an empty string if searchText or the second space is not found
        return "";
    }
}
