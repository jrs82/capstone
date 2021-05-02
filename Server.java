/**
 * Purpose: To host and manage a chat room for clients on the Internet
 * This file contains inner classes which are components of the Server-side system
 * We decided 2 java files were better than 12 because it's more intuitive what code should be on Server machine versus Client machine
 * Client.java communicates with Server.java on port 60001 to create a stealth mode chat environment
 * Client users can register accounts and send encrypted messages back and forth to Server-side chat room.
 * @author Johnny Mitchell, Adam Prusiecki, Joshua Simpson, Marcus Truex
 */
package capstone;

/**
 * Import everything the Server side machine will need to manage the chat.
 */
import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.DefaultCaret;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.util.Base64;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import javax.crypto.SecretKey;

/**
 * Server class connects and manages up to 100 users (clients) to a chat room.
 */
public class Server extends JFrame implements ActionListener {	
    //components
    private int counter;
    private static final HashMap<String,PrintWriter> activeUsers=new HashMap<>(); //the list of users logged in to the chat
    private final JButton btnServer;
    private final JLabel lblTitle;
    private final JPanel topPanel;
    private final JPanel midPanel;
    private final JScrollPane scrollPane;
    private final JTextArea chat;
    private static final int PORT = 60001;
    private static ServerSocket serverSocket;
    private static String rsaStringKey;
    //private static byte[] decodedByteKey;
    //private static PublicKey passedPublicKey;
    private static MessageHandler mh;
    //private static SecretKey aesKey;
    //private static byte[] keyEncryptedByUserPublicKey;
    //private static byte[] keyEncryptedByPassedKey;
    //private static SecretKey decryptedUserKey;
    //private static SecretKey decryptedPassedKey;
    //private static String encryptedUserMsg;
    //private static String decryptedUserMsg;
    //private static String encryptedPassedMsg;
    //private static String decryptedPassedMsg;
    private static String[] s;
    private static String s1;
    private static final Map<Character, Character> map=new HashMap<>();
    private static char[] chars;
    
    /**
     * Constructs a server GUI.
     */
    public Server() {
        super("MUC Server");
        counter=0;//for look and feel. counts button clicks.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(180, 140, 435, 380); //look and feel. remember we aren't centering this jframe on the screen like we are with the client.java
        topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(7, 7, 1, 7));//look and feel
        topPanel.setLayout(new BorderLayout());
        midPanel = new JPanel();
        midPanel.setBorder(new EmptyBorder(7, 7, 7, 7));//look and feel
        midPanel.setLayout(new BorderLayout());
        lblTitle = new JLabel("MUC Server (offline)");
        lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
        lblTitle.setFont(new Font("Verdana", Font.ITALIC, 20));
        topPanel.add(lblTitle, BorderLayout.WEST);
        btnServer = new JButton("START SERVER");
        btnServer.addActionListener((ActionEvent e)->{
            //decide how the server button works
            if(btnServer.getText().equals("START SERVER")){
                if(counter>0){
                    writeToChat("Server is back online.");
                }//end inner if
                counter++;
                lblTitle.setText("MUC Server (online)");
                btnServer.setText("STOP SERVER");
                ConnectionListener conn=new ConnectionListener();//listen for incoming connections
                new Thread(conn).start();//start new threads when incoming connections are heard
            }else{//end outer if
                writeToChat("Server went offline.");
                lblTitle.setText("MUC Server (offline)");
                btnServer.setText("START SERVER");
                try{
                    serverSocket.close();
                }catch(IOException ex){//end try
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }//end try/catch
                System.exit(0);
            }//end else
        });//end btnServer actionlistener lambda expression
        btnServer.setFont(new Font("Verdana", Font.PLAIN, 20));
        topPanel.add(btnServer, BorderLayout.EAST);
        scrollPane = new JScrollPane();
        midPanel.add(scrollPane, BorderLayout.CENTER);
        chat = new JTextArea();
        chat.setLineWrap(true);//words will wrap around instead of run off the side
        chat.setWrapStyleWord(true);//wrap after a whole word and not just a single character
        scrollPane.setViewportView(chat);//allows the chat to be scrollable
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(midPanel, BorderLayout.CENTER);
    }//end Server constructor
    
    @Override//Note: Java didn't like my lambda expression on my button in the constructor even though it works perfectly.
    public void actionPerformed(ActionEvent ae) {
        throw new UnsupportedOperationException("Button properly configured with lambda expression inside constructor.");
    }//end override button action event
    
    /**
     * main() method creates a chat room and redirects SysOut traffic to it.
     * @param args
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Server gui = new Server();
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                SwingUtilities.updateComponentTreeUI(gui);
                //set it up to scroll to the bottom of the chat so you don't have to
                DefaultCaret caret = (DefaultCaret) gui.chat.getCaret();
                caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                //write to the chat room
                System.setOut(new PrintStream(new Chat(gui.chat)));
                gui.setVisible(true);
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                System.out.println("ERROR: "+e.getMessage());
            }//end try/catch
        });//end EventQueue lambda expression
    }//end main()

    /**
     * write a user input string to the chat room.
     * @param str 
     */
    private static void writeToChat(String str){
        if(str.contains("%")){//handle any key
            s=str.split("%",2);
            s1=s[1];
            if(s[1].contains("%")){
                map.put('%', '*');//change all % to *
                chars=s1.toCharArray();
                for(int i=0;i<s1.length();i++){
                    chars[i]=map.get(chars[i]);
                }
                s1=String.valueOf(chars);
            }
            //s[1] is the encryptedkey at this point. s1 may be the key but may also be an asterisk due to the above if statement.
            rsaStringKey=s[1];//get a copy of the key
            byte[] decodedByteKey=Base64.getDecoder().decode(rsaStringKey); //server side
            PublicKey passedPublicKey = null;
            try{
                KeyFactory kf = KeyFactory.getInstance("RSA");
                EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedByteKey);
                passedPublicKey = kf.generatePublic(keySpec);
            }catch(NoSuchAlgorithmException | InvalidKeySpecException e){
                System.out.println("ERROR: "+e.getMessage());
            }
        }else{
            //buggy encryption code
            /*
            UserKeys a = new UserKeys();
            byte[] decodedByteKey=Base64.getDecoder().decode(rsaStringKey); //server side
            PublicKey passedPublicKey = null;
            try{
                KeyFactory kf = KeyFactory.getInstance("RSA");
                EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedByteKey);
                passedPublicKey = kf.generatePublic(keySpec);
            }catch(NoSuchAlgorithmException | InvalidKeySpecException e){
                System.out.println("ERROR: "+e.getMessage());
            }
            MessageHandler mh = new MessageHandler();
            SecretKey aesKey = a.GetAesKey();
            byte[] keyEncryptedByPassedKey = mh.EncryptKey(aesKey,passedPublicKey);
            SecretKey decryptedPassedKey = mh.DecryptKey(keyEncryptedByPassedKey,a.GetMyPrivateKey());
            String encryptedPassedMsg = str;
            String decryptedPassedMsg = mh.DecryptString(encryptedPassedMsg,decryptedPassedKey);
            */
            
//the encryption/decryption has some bugs in it still. let's go without, for the demo.
            System.out.println(str);//post a copy of the message(and username) in the server chat
            //have all the users who are currently typing place their messages in the chat
            activeUsers.values().forEach((p)->{
                p.println(str);//write to chat any message any user said.
            });//end for-each activeUsers lambda expression
        }//end else
    }//end writeToChat method

    /**
     * Listens for incoming user connections.
     */
    private static class ConnectionListener implements Runnable{
        @Override//turn all users into threads
        public void run(){
            try{
                serverSocket=new ServerSocket(PORT);
                System.out.println("Listening for client connections on port "+PORT);
                while(true){//listen forever
                    if(activeUsers.size()<100){ //allow only 100 connections (threads) on the server at once
                        ConnectionManager conn=new ConnectionManager(serverSocket.accept()); //handle each new client
                        new Thread(conn).start();//turn each new client into a Thread
                    }//end if
                }//end while
            }catch(IOException e){//end try
                if(e.getMessage().contains("socket closed")){//ignore annoying error messages in certain cases
                    System.out.println("Socket already closed.");
                }else{//end if
                    System.out.println("ERROR: "+e.getMessage());
                }//end else
            }finally{//end try/catch
                try {//close all connections
                    serverSocket.close();
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }//end try/catch
            }//end finally
        }//end override run
    }//end class ConnectionListener

    /**
     * Manages all incoming user connections and adds them to Server's HashMap.
     */
    private static class ConnectionManager implements Runnable{
        private BufferedReader in;
        private PrintWriter out;
        private final Socket user;
        private String username;

        /**
         * Construct a new user connection.
         * @param socketConnection 
         */
        public ConnectionManager(Socket socketConnection){
            this.user=socketConnection;
        }//end constructor

        @Override//tell the server the user info
        public void run(){
            try{//put the new user in the existing hashmap
                System.out.println("Managing new user from "+user.getInetAddress());
                in=new BufferedReader(new InputStreamReader(user.getInputStream()));
                out=new PrintWriter(user.getOutputStream(),true);
                while(true){//read every line forever
                    username=in.readLine();
                    synchronized(activeUsers){
                        //handle situations where a user tries to login as someone who is already logged in
                        if(activeUsers.keySet().contains(username)){
                            Random randomGuest=new Random();
                            int randomInteger=randomGuest.nextInt(999999);
                            username="Guest"+randomInteger;
                            out.println("That user is already logged in. Logging in as "+username+".");
                            break;
                        }else{//end if
                            break;
                        }//end else
                    }//end sync
                }//end while
                activeUsers.put(username, out); //that user's output stream is now associated with that username in the activeUsers hashmap
                getWelcomeMessage();
                System.out.println(username+" logged in.");
                writeToChat(username+" joined the chat.");
                String str;//the user text input
                while((str=in.readLine())!=null) {
                    if (!str.isEmpty()) {//the user actually wrote something
                        if (str.toLowerCase().equals("/quit")) {//user is done chatting.
                            break;//in this case, don't write, just leave the while loop and go to the finally block
                        }//end inner if
                        //actually write the user's text in the chat
                        writeToChat(username+": "+str);
                    }//end if
                }//end while
            }catch(IOException e){//end try
                if(e.getMessage().contains("Connection reset")){//active user hard-exited client via X button or Exit button.
                    System.out.println(username+" exited the client.");//...so remove name in finally block
                }else{//end if
                    System.out.println("ERROR: "+e.getMessage());
                }//end else
            }finally{//end try/catch
                activeUsers.remove(username); //remove the user from the hashmap when finished
                writeToChat(username+" left the chat."); //tell chatroom the user left
                System.out.println(username+" logged out."); //tell server the user logged out
            }//end finally
        }//end run override

        /**
         * Literally just welcome the user to the chat with a custom message.
         * @note Each user sees a different custom message at login time. Everyone else in the chat just sees Username joined the chat.
         */
        private void getWelcomeMessage() {
            String message;
            Random randomMessage = new Random();
            int randomInteger=randomMessage.nextInt(18);
            switch(randomInteger){
                case 0: message="Welcome to the chat room, "+username+"!";break;
                case 1: message=username+" is being escorted to the chat by elite ninja warriors.";break;
                case 2: message="It's about time you showed up, "+username+"!";break;
                case 3: message=username+", you're here! Where's my money?";break;
                case 4: message="Uh oh, "+username+" is here! Lay out the red carpet.";break;
                case 5: message=username+", welcome to the chat! I saw what you did. Don't worry, I won't tell anyone.";break;
                case 6: message=username+", you're here! There's still some cake left.";break;
                case 7: message=username+", grab a party hat. It's a party!";break;
                case 8: message="Welcome, "+username+"! If you've come here to talk about your problems, that'll be $70/hour.";break;
                case 9: message="Welcome, "+username+"! Grab a shovel. The last guy didn't make it.";break;
                case 10: message="Dormammu... I mean, "+username.toUpperCase()+", I've come to bargain!";break;
                case 11: message="Welcome, "+username+"! I hope you brought snacks.";break;
                case 12: message=username+"! There you are! We've been looking all over.";break;
                case 13: message=username+", I guess you can enter the chat just don't touch my horse.";break;
                case 14: message="Rumor has it, "+username+" is coming. Run for your lives!";break;
                case 15: message="It's just down that dark alley, "+username+".";break;
                case 16: message="Welcome to the chat, "+username+"! Don't mind Jim. He won't bite.";break;
                case 17: message=username+", I hope you're ready for a RAP BATTLE. Get in there!";break;
                default: message="Ya done goofed, "+username+"!";break;
            }
            out.println(message);//put the random message in the outputstream
            try { //look and feel
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }//look and feel
        }//end getWelcomeMessage method
    }//end class ConnectionManager
    
/**
 * Note: The following classes, Chat, UserKeys, and MessageHandler
 * are needed by the Server program and also the Client program
 * so it is the same code at the bottom of each program because
 * theoretically the Client and Server machines will be in different
 * locations so we want to maintain complete code access in 2+ locations.
 */

    /**
     * Chat class builds string out of user input and writes it in the actual chat.
     */
    private static class Chat extends OutputStream{
        private final JTextArea chatRoom;
        private final StringBuilder chatString;
        
        /**
         * Constructs a chat.
         * @param chatMessages a collection of messages in the chat 
         */
        public Chat(JTextArea chatMessages) {
            this.chatRoom=chatMessages;
            this.chatString=new StringBuilder();
        }//end constructor
        
        @Override//actually write the user text in the chat
        public void write(int character) {//pass in the integer version of the character typed
            switch(character){
                case '\n':
                    //write a newly built string to the chatroom
                    chatRoom.append(chatString.toString()+"\n");
                    //empty the stringbuilder starting at index 0 so we can build the next string
                    chatString.delete(0,chatString.length());
                    //now work on the next message.
                    break;
                case '\r':
                    //Some OSes use '\n' while others use '\r\n' together. 
                    //This case intentionally does nothing when it finds an \r since \r is always followed by \n anyway
                    break;
                default:
                    //build the string to be displayed in chat one character at a time.
                    chatString.append((char)character);
            }//end switch logic
        }//end override write method
    }//end Chat class
    
    /**
     * UserKeys class used for encryption.
     */
    public static final class UserKeys
    {
        private SecretKey aesKey;
        private PublicKey myPublicKey;
        private PrivateKey myPrivateKey;
        private PublicKey foreignPublicKey;

        /**
         * Constructs a UserKeys object that has several keys: AesKey, PublicKey, PrivateKey.
         */
        public UserKeys()
        {
            this.SetAesKey();
            this.GenerateRsaKeys();
        }//end constructor

        /**
         * AesKey setter.
         */
        public void SetAesKey()
        {
            try
            {   //try to make a random key for encryption/decryption
                SecureRandom secureRandom = new SecureRandom();
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(128,secureRandom);
                this.aesKey = keyGen.generateKey();
            }//end try
            catch(NoSuchAlgorithmException e)
            {
                System.out.println("Error generating key: "+e.getMessage());
            }//end try/catch
        }//end set AesKey

        /**
         * AesKey setter using SecretKey parameter.
         * @param thisKey the SecretKey
         */
        public void SetAesKey(SecretKey thisKey)
        {
            this.aesKey = thisKey;
        }//end set AesKey(SecretKey)

        /**
         * AesKey getter.
         * @return the AesKey
         */
        public SecretKey GetAesKey()
        {
            return this.aesKey;
        }//end get AesKey

        /**
         * Generate an RSA key pair.
         */
        public void GenerateRsaKeys()
        {
            try
            {   //try to make a RSA key "pair" for encryption/decryption
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(4096);
                KeyPair pair = keyGen.generateKeyPair();
                this.myPrivateKey = pair.getPrivate();
                this.myPublicKey = pair. getPublic();
            }//end try
            catch(NoSuchAlgorithmException e)
            {
                System.out.println("Error generating key pair: "+e.getMessage());
            }//end try/catch
        }//end Generate RSA key pair method

        /**
         * PublicKey getter.
         * @return the public key
         */
        public PublicKey GetMyPublicKey()
        {
            return this.myPublicKey;
        }//end get PublicKey

        /**
         * PrivateKey getter.
         * @return the private key
         */
        public PrivateKey GetMyPrivateKey()
        {
            return this.myPrivateKey;
        }//end get PrivateKey

        /**
         * ForeignPublicKey setter.
         * @param foreignPublicKey 
         */
        public void SetForeignPublicKey(PublicKey foreignPublicKey)
        {
            this.foreignPublicKey = foreignPublicKey;
        }//end set ForeignPublicKey

        /**
         * ForeignPublicKey getter.
         * @return the foreign public key
         */
        public PublicKey GetMyForeignPublicKey()
        {
            return this.foreignPublicKey;
        }//end get ForeignPublicKey
    }//end class UserKeys
    
    /**
     * MessageHandler class handles any encryption/decryption.
     */
    public static class MessageHandler 
    {
        /**
         * Just use an empty constructor for now.
         */ 
        public MessageHandler()
        {
            //don't need anything here
        }//end constructor

        /**
         * Encrypt a string.
         * @param plainText the user's original text string
         * @param secretKey the encryption key
         * @return the encrypted string
         */
        public String EncryptString(String plainText,SecretKey secretKey)
        {
            String encryptedString = "Cannot Encrypt String";//default value for when encryption fails
            try
            {
                Cipher c = Cipher.getInstance("AES"); //look ECB vs CBC
                c.init(Cipher.ENCRYPT_MODE,secretKey);
                encryptedString = Base64.getEncoder().encodeToString(c.doFinal(plainText.getBytes("UTF-8")));
            }//end try
            catch(UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e)
            {
                System.out.println("Error: "+e.getMessage());
            }//end try/catch
            return encryptedString;//return the encrypted string
        }//end method EncryptString

        /**
         * Decrypt a string.
         * @param encryptedString the secure encrypted string the system generated
         * @param secretKey the decryption key
         * @return the decrypted string
         */
        public String DecryptString(String encryptedString,SecretKey secretKey)
        {
            String decryptedString = "Cannot Decrypt String";//default value for when decryption fails
            try
            {
                Cipher c = Cipher.getInstance("AES");
                c.init(Cipher.DECRYPT_MODE,secretKey);
                decryptedString = new String(c.doFinal(Base64.getDecoder().decode(encryptedString)));
            }//end try
            catch(InvalidKeyException | NoSuchAlgorithmException | BadPaddingException | IllegalBlockSizeException | NoSuchPaddingException e)
            {
                System.out.println("Error: "+e.getMessage());
            }//end try/catch
            return decryptedString;//return the decrypted string
        }//end method DecryptString

        /**
         * Encrypt the actual key for security.
         * @param secretKey the code we'll need to make an encrypted key
         * @param pubKey the public key we're encrypting
         * @return the byte[] object "returnKey" which is made from the secretKey and publicKey
         */
        public byte[] EncryptKey(SecretKey secretKey,PublicKey pubKey)
        {
            byte[] returnKey = {0x0};//make the blank byte[] object
            try
            {
                Cipher c = Cipher.getInstance("RSA");
                c.init(Cipher.WRAP_MODE,pubKey);
                returnKey = c.wrap(secretKey);//feed it a publicKey and a secretKey
            }//end try
            catch(InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | NoSuchPaddingException e)
            {
                System.out.println("Error: "+e.getMessage());
            }//end try/catch
            return returnKey;//return the encrypted key
        }//end method EncryptKey

        /**
         * Decrypt an encrypted key. 
         * @note Necessary for decrypting messages later.
         * @param EncryptedKey the key we want to decrypt (a byte[] object)
         * @param priKey the code to decrypt the key we want
         * @return the decrypted key
         */
        public SecretKey DecryptKey(byte[] EncryptedKey,PrivateKey priKey)
        {
            SecretKey sKey = null;
            try
            {
                Cipher c = Cipher.getInstance("RSA");
                c.init(Cipher.UNWRAP_MODE,priKey);
                sKey = (SecretKey)c.unwrap(EncryptedKey,"AES",Cipher.SECRET_KEY);
            }//end try
            catch(InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e)
            {
                System.out.println("Error: "+e.getMessage());
            }//end try/catch
            return sKey; //return the decrypted key. now we can use this secret key to privately decrypt messages.
        }//end method DecryptKey
    }//end class MessageHandler
}//end Server.java
