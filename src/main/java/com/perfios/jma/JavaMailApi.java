package com.perfios.jma;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
 
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import java.util.Stack;

public class JavaMailApi extends Base{
	
	/**
	 * Variable: hOSTING IMAP and Email Credentials
	 */
	private static final String PROPERTY_FILE = System.getProperty("user.dir") + 
			File.separator + "src" + 
			File.separator + "main" + 
			File.separator + "resources" + 
			File.separator + "application.properties";

	public static void main(String[] args) {
		
		readValidateText("Welcome to Perfios Insights!");
		String getLink = readGetLink("Change your Perfios Insights password");
		
		driver.get(getLink);
	}
	
	/**Method: To Read the email and validate text present in the body
	 * @param subject
	 */
	public static void readValidateText(String subject) {
		 
        Properties props = new Properties();
 
        try {
            props.load(new FileInputStream(new File(PROPERTY_FILE)));
            Session session = Session.getDefaultInstance(props, null);
 
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", props.getProperty("username"), props.getProperty("password"));
         
            Folder inbox = store.getFolder("inbox");
            inbox.open(Folder.READ_ONLY);
            int messageCount = inbox.getMessageCount();
 
            System.out.println("Total Messages:- " + messageCount);
 
            Message[] messages = inbox.getMessages();
            System.out.println("------------------------------");
            String body;
            Stack<String> stk= new Stack<String>(); 
 
            //Matching Subject pushing it to Stack
            for (int i = (messages.length - 1); i > (messages.length - 10); i--) {
            	
            	String match = messages[i].getSubject();
            	
            	if (match.equals(subject)) {
            	body = 	(String) messages[i].getContent();
            	stk.push(body);
				}
            }
            
            //Removing Duplicate Emails containing same content
            for (int i = stk.size(); i > (stk.size() - (stk.size() - 1)); i--) {
				stk.pop();
			}
            
            String stackBody = stk.toString();
            if(stackBody.contains("Password \"<b>falcon123</b>\"")) {
            	System.out.println("Password Matched");
            }
            else {
				System.out.println("Password Not Matched");
			}
           
            inbox.close(true);
            store.close();
 
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	
	/**Method: To Read the email and get the Reset Password link present in the body
	 * @param subject
	 * @return Link URL
	 */
	public static String readGetLink(String subject) {

		 Properties props = new Properties();
		 String link = null;
		 
	        try {
	            props.load(new FileInputStream(new File(PROPERTY_FILE)));
	            Session session = Session.getDefaultInstance(props, null);
	 
	            Store store = session.getStore("imaps");
	            store.connect("imap.gmail.com", props.getProperty("username"), props.getProperty("password"));
	         
	            Folder inbox = store.getFolder("inbox");
	            inbox.open(Folder.READ_ONLY);
	            int messageCount = inbox.getMessageCount();
	 
	            System.out.println("Total Messages:- " + messageCount);
	 
	            Message[] messages = inbox.getMessages();
	            System.out.println("------------------------------");
	            String body;
	            Stack<String> stk= new Stack<String>(); 
	 
	            //Matching Subject pushing it to Stack
	            for (int i = (messages.length - 1); i > (messages.length - 10); i--) {
	            	
	            	String match = messages[i].getSubject();
	            	
	            	if (match.equals(subject)) {
	            	body = 	(String) messages[i].getContent();
	            	stk.push(body);
					}
	            }
	            
	            //Removing Duplicate Emails containing same content
	            for (int i = stk.size(); i > (stk.size() - (stk.size() - 1)); i--) {
					stk.pop();
				}
	            
	            //Retrieving http url from the email Body
	            String stackBody = stk.toString();
	            String arr[] = stackBody.split("\n");
	            
				for (String s : arr) {
					s = s.trim();
					if (s.startsWith("http") || s.startsWith("https")) {
						
						link = s.trim();
					}
				}
	            inbox.close(true);
	            store.close();
	            
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	        return link;
	}
}