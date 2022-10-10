package org.example;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RecognizeEventSignature {
    public static void recognizeLogFile(String jsonFilePath, String logFilePath) {
        File jsonFile = new File(jsonFilePath);
        // eventMap is used to store the event correspondence
        // Integer represents the event ID (here, it starts from 0, which is easy to retrieve from the array)
        // String represents the type of the event (that is, the enter function of the event signature)
        HashMap<Integer, String> eventMap = new HashMap<>();
        // ja is used to store event's jsonobjects
        JSONArray ja = new JSONArray();
        if(jsonFile.isFile() && jsonFile.exists() && jsonFile.length() != 0) {
            try {
                String content = FileUtils.readFileToString(jsonFile, "UTF-8");
                JSONObject jo = JSONObject.parseObject(content, Feature.OrderedField);
                ja = jo.getJSONArray("event");
                for (int i = 0; i < ja.size(); i++) {
                    JSONObject object = ja.getJSONObject(i);
                    eventMap.put(i, (String) object.get("type"));
//                    System.out.println(object.get("type"));
                }
            } catch (Exception e) {
                System.err.println("Reading File Error!");
                System.err.println(e);
            }
        }
        File logFile = new File(logFilePath);
        if(logFile.isFile() && logFile.exists() && logFile.length() != 0) {
            try {
                InputStreamReader Reader = new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(Reader);
                String line;
                // Store method call chain
                List<String> methodChain = null;
                // tag：whether it needs to be written to the call chain
                boolean tag = false;
                // The type of the current event (enter function)
                String type = "";
                while ((line = bufferedReader.readLine()) != null) {
                    // If the corresponding type is found, first take out the event of this event
                    if(!tag && eventMap.containsValue(line)) {
                        tag = true;
                        type = line;
                        methodChain = new LinkedList<>();
                    }
                    if(tag) {
                        methodChain.add(line);
                    }
                    // Record the end of the method call chain
                    if(line.equals("[" + type + "]")) {
                        // The type may be repeated, so compare all the signatures of the same type
                        for(Integer key: eventMap.keySet()){
                            if(eventMap.get(key).equals(type)){
                                // Take out the corresponding method call chain
                                List<String> signature = (List<String>)ja.getJSONObject(key).get("signature");
                                if(signature.equals(methodChain)) {
                                    int id = key + 1;
                                    System.out.println("Event " + ja.getJSONObject(key).get("ID") + ": " + type + " has been executed!");
                                }
                            }
                        }
                        tag = false;
                        type = "";
                    }
                }
            } catch (Exception e) {
                System.err.println("Reading File Error!");
                System.err.println(e);
            }
        }
    }

    public static void main(String[] args) {
        String jsonFilePath = "event_signature.json";
        String logFilePath = "coverage2.txt";
        recognizeLogFile(jsonFilePath, logFilePath);
    }
}
