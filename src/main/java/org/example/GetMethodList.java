package org.example;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.*;

public class GetMethodList {
    public static Set<String> s = new HashSet<>();
    public static final String APP_PACKAGE_NAME = "com/ichi2/anki";
    public static final String NOT_HANDLER_METHOD = "onCreate|onStart|onResume|onPause|onStop|onDestroy|onSaveInstanceState|onPreExecute|onPostExecute|onLoadFinished|onBindViewHolder|onAttachedToWindow|onProgressUpdate|onActivityResult";

    public static void readTxt(String filePath) {
        // Create sqlite-jdbc connection
        try (var c = DriverManager.getConnection("jdbc:sqlite::memory:")){
            var stmt = c.createStatement();
            stmt.executeUpdate("restore from listener.db");
            System.out.println("Opened database successfully！");
            File file = new File(filePath);
            JSONObject objectAll = new JSONObject(new LinkedHashMap<>());
            JSONArray array = new JSONArray();
            if(file.isFile() && file.exists() && file.length() != 0) {
                try(InputStreamReader Reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                    BufferedReader bufferedReader = new BufferedReader(Reader)){
                    String line;
                    // tag: Whether the current method is the one we are interested in
                    boolean tag = false;
                    String head = "";
                    long startTime = System.currentTimeMillis();
                    int count = 1;
                    List<String> methodChain = null;
                    while ((line = bufferedReader.readLine()) != null) {
                        String methodName = line.substring(line.lastIndexOf("/") + 1);
//                        if(methodName.matches(NOT_HANDLER_METHOD)){
//                            continue;
//                        }
                        String special = "com/ichi2/anki/NavigationDrawerActivity\\$4/run|com/amaze/filemanager/ui/views/appbar/SearchView/lambda\\$new\\$2";
                        if (!tag){
//                            String sql = "select * from event_handler_method where name='" + methodName + "' and name not like '%on%Create%'";
                            if(methodName.startsWith("on") || methodName.equals("afterTextChanged") || methodName.equals("verseLongPress")){
                                String sql = "select * from themis_method where name='" + methodName + "'";
                                ResultSet rs = stmt.executeQuery(sql);

                                if (line.startsWith(APP_PACKAGE_NAME) && rs.next()) {
                                    tag = true;
                                    head = line;
                                    System.out.println("Now instrumenting：" + line);
                                    methodChain = new LinkedList<>();
                                }
                            }
                            else if(line.matches(special)){
                                tag = true;
                                head = line;
                                System.out.println("Now instrumenting：" + line);
                                methodChain = new LinkedList<>();
                            }

                        }
                        if (tag) {
                            methodChain.add(line);
                            if(!line.equals("[" + head + "]")){
                                if(!line.startsWith("[")){
                                    s.add(line);
                                }
                            }
                            else {
                                tag = false;
                                // flag：whether need to write the event signature into the file, avoiding duplication
                                boolean flag = false;
                                for(int i = 0; i < array.size(); i++){
                                    JSONObject ob = array.getJSONObject(i);
                                    if(head.equals((String)ob.get("type")) && methodChain.equals((List<String>)ob.get("signature"))) {
                                        flag = true;
                                        break;
                                    }
                                }
                                if(!flag){
                                    JSONObject object = new JSONObject(new LinkedHashMap<>());
                                    object.put("ID" , count);
                                    object.put("type", head);
                                    object.put("signature", methodChain);
                                    array.add(object);
    //                                objectAll.put("event" + count, object);
                                    count++;
                                }
                                head = "";
                            }
                        }
                    }
                    File destFile = new File("./MethodList.txt");
                    try (var of = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(destFile, true)))){
                        for (String str: s) {
                            of.write(str + "\n");
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
//                    System.out.println(object);
                    JSONObject object2 = new JSONObject(new LinkedHashMap<>());
                    object2.put("event", array);
                    OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream("event_signature.json"),"UTF-8");
//                    osw.write(objectAll.toString());
                    osw.write(object2.toString());
                    osw.flush();
                    osw.close();
                    long endTime = System.currentTimeMillis();
                    long time = endTime - startTime;
                    System.out.println("Instumentation costs: " + time + "ms");
                }
            } else {
                System.out.println("Can't find the target file!");
            }
        } catch (Exception e) {
            System.err.println("Reading File Error!");
            System.err.println(e);
        }
    }

    public static void main(String[] args) {
        String filePath = "D:\\instruction-study\\Driod-store\\Anki-Android-bug-4451\\coverage.txt";
        readTxt(filePath);
    }
}
