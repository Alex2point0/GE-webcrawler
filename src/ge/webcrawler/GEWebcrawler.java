/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ge.webcrawler;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;


/**
 *
 * @author Kyle
 */

/** note: one class holds the maps and lists while the other does the crawl. This is to reduce overhead of reinstantiating, reading the json, and running loops to fill the map
 * every time a thread recursively calls the crawl class. Rather, the class with the objects is held by the class that crawls for quick retrieval.
 *
 * 
 */
public class GEWebcrawler  { 
    final List success = Collections.synchronizedList(new ArrayList<Object>()); //unsure of capacity so will use a list
    final List error = Collections.synchronizedList(new ArrayList<Object>());
    final List skipped = Collections.synchronizedList(new ArrayList<Object>());
    final Map<Object, JSONArray> jsonMap = new LinkedHashMap<>(); //key = address page, value = corresponding links array

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     * @throws org.json.simple.parser.ParseException
     */
    public static void main(String[] args) throws IOException, FileNotFoundException, ParseException, InterruptedException{
        // TODO code application logic here                              
        GEWebcrawler ge = new GEWebcrawler();
        Object randomPage = ge.jsonMap.keySet().toArray()[new Random().nextInt(ge.jsonMap.keySet().toArray().length)]; //choose a random page to start crawl
        Thread crawl = new Thread(new Crawl(randomPage, ge)); //begin first crawl thread                    
        crawl.start();
        crawl.join();
        
        System.out.println("Success:\n" + ge.getSuccess());
       
        System.out.println("Skipped: \n" + ge.getSkipped());
        
        System.out.println("Error: \n" + ge.getError());
    }
         
    GEWebcrawler() throws IOException, FileNotFoundException, ParseException{        
        JSONObject obj = readJSON();
        createMap((JSONArray) obj.get("pages"));
       
    }
     
    private void createMap(JSONArray Pages){        
        for (Object o : Pages){            
            JSONObject pages = (JSONObject) o; //explicit type cast to JSONObject
            Object address =  pages.get("address"); 
            JSONArray links = (JSONArray) pages.get("links"); //explicit typecast "links" object to JSONArray
            jsonMap.put(address, links);        //key = address page, value = corresponding links array
        }        
    }
    
    private JSONObject readJSON() throws FileNotFoundException, IOException, ParseException{
        Object obj = new JSONParser().parse(new FileReader("internet_1.json"));
        JSONObject jo = (JSONObject) obj;
        return jo;
    }
           
    void crawlWithoutMultiThreading(Object page){      //first attempt before using multithreading   
        if (!jsonMap.containsKey(page)){
            System.out.println("Invalid Page");; //first page is erroneous
            return;
        }
        
        success.add(page); //if crawl() is called, a page was successfully visited for the first time
        JSONArray links = jsonMap.get(page); //links array for page key

        for (Object i : links){ //loops through pages in links array
            if (!jsonMap.containsKey(i)) error.add(i); //if page does not exist, add to error
            else if (!success.contains(i)) crawlWithoutMultiThreading(i);           //if page has not been visited, call crawl with new page
            else {
                skipped.add(i); //page has been visited before
            }            
        }           
    }
    
    List getError(){
        return this.error;
    }
    
    List getSkipped(){
        return this.skipped;
    }
    
    List getSuccess(){
        return this.success;
    }
       
}

class Crawl implements Runnable {
    Object page;
    GEWebcrawler ge;

    public Crawl(Object page, GEWebcrawler ge) throws IOException, FileNotFoundException, ParseException{
        this.page = page; 
        this.ge = ge;     //holds list objects from instance of ge
    }

    public void run(){ //multithreading crawl
        if (!ge.jsonMap.containsKey(page)){
            System.out.println("Invalid Page"); //first page is erroneous
            return;           
        }
        
        ge.success.add(page); //if crawl() is called, a page was successfully visited for the first time
        JSONArray links = ge.jsonMap.get(page); //links array for page key

        for (Object i : links){ //loops through pages in links array            
            if (!ge.jsonMap.containsKey(i)) ge.error.add(i); //if page does not exist, add to error
            else if (!ge.success.contains(i)) 
                try {                             
                    Thread crawl = new Thread(new Crawl(i, ge)); //if page has not been visited, call crawl with new page and ge class object               
                    crawl.start(); //start new thread
                    crawl.join(); //waits for thread to end
                    
                } catch (IOException | ParseException ex) {
                    Logger.getLogger(GEWebcrawler.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                Logger.getLogger(Crawl.class.getName()).log(Level.SEVERE, null, ex);
            }
            else {
                ge.skipped.add(i); //page has been visited before
            }            
        }        
    }
    
}


