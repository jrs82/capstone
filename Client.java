/**
 * Purpose: To allow users (clients) to chat with each other through a server (in a chat room the server hosts)
 * This file contains inner classes which are components of the Client-side system
 * We decided 2 java files were better than 12 because with only 2 it's easy to see which code belongs on Client machine and which belongs on Server
 * Client.java communicates with Server.java on port 60001 to create a stealth mode chat environment
 * Client users can register accounts and send encrypted messages back and forth to Server-side chat room.
 * @author Johnny Mitchell, Adam Prusiecki, Joshua Simpson, Marcus Truex
 */
package capstone;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/**
 * Create a client GUI.
 */
public class Client extends JFrame implements ActionListener {
    private static final int PORT = 60001;
    private final JButton exitButton;
    private final JButton loginButton;
    private final JButton registerButton;
    private final JButton sendButton;
    private final JLabel passwordLabel;
    private final JLabel usernameLabel;
    private final JPanel gui;
    private final JPanel topBanner;
    private final JPanel bottomBanner;
    private final JPasswordField passwordField;
    private final JScrollPane chatArea;
    private final JTextArea chatText;
    private final JTextField userText;
    private static JTextField usernameField;
    private PrintWriter out;
    private static Socket clientSocket;
    private static String clientName;
    private static String todaysFace;
    private static PublicKey userPublicKey;
    private static UserKeys a;
    //private static MessageHandler mh;
    //private static SecretKey aesKey;
    //private static byte[] keyEncryptedByUserPublicKey;
    //private static byte[] keyEncryptedByPassedKey;
    //private static SecretKey decryptedUserKey;
    //private static SecretKey decryptedPassedKey;

    /**
     * Construct the Client-side JFrame.
     */
    public Client() {
        super("Chat Client   "+todaysFace);//custom Lenny face for each user
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0, 0, 550, 340); //we're centering the window on the screen later anyway...
        usernameLabel=new JLabel("Username");
        usernameField=new JTextField();
        usernameField.setColumns(8);//8 is wide enough. it can scroll anyway.
        passwordLabel=new JLabel("Password");
        passwordField=new JPasswordField();
        passwordField.setColumns(8);
        loginButton=new JButton("LOGIN");//login an existing account
        getRootPane().setDefaultButton(loginButton);//the first thing the user does is login, duh.
        registerButton=new JButton("REGISTER");//register a new account
        exitButton=new JButton("EXIT");//exit the client
        userText=new JTextField();//where the user types to the chatroom
        userText.setColumns(50);
        sendButton=new JButton("SEND");//send the message
        loginButton.setFont(new Font("Verdana", Font.BOLD, 10));//look and feel
        registerButton.setFont(new Font("Verdana", Font.BOLD, 10));
        exitButton.setFont(new Font("Verdana", Font.BOLD, 10));
        sendButton.setFont(new Font("Verdana", Font.BOLD, 10));
        //add ActionListener to Login button
        loginButton.addActionListener((ActionEvent e)->{
            if(loginButton.getText().equals("LOGIN")) {
                try {
                    login();
                } catch (InvalidKeySpecException | NoSuchAlgorithmException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }else{
                logout();
            }
        });//end loginButton actionlistener lambda expression
        //add ActionListener to Register button
        registerButton.addActionListener((ActionEvent e)->{
            MucRegister registrationWindow=new MucRegister();
            try {//try to set the Register GUI to Windows OS look and feel
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }//end try/catch
            SwingUtilities.updateComponentTreeUI(registrationWindow);
            registrationWindow.setLocationRelativeTo(null);
            registrationWindow.setVisible(true);
        });//end registerButton actionlistener lambda expression
        //add ActionListener to Exit button
        exitButton.addActionListener((ActionEvent e)->{ 
            System.exit(0);
        });//end exitButton actionlistener lambda expression
        //add ActionListener to Send button
        sendButton.addActionListener((ActionEvent e)->{
            String str = userText.getText().trim();//whatever text the user entered in the GUI
            if(str.contains("%")){
                str=""; //we're not letting users type %
            }
            if(!str.isEmpty()) {//if string not empty
                //encrypt it!
                /*
                MessageHandler mh = new MessageHandler();
                SecretKey aesKey = a.GetAesKey();
                byte[] keyEncryptedByUserPublicKey = mh.EncryptKey(aesKey,userPublicKey);
                SecretKey decryptedUserKey = mh.DecryptKey(keyEncryptedByUserPublicKey,a.GetMyPrivateKey());
		String encryptedUserMsg = mh.EncryptString(str,decryptedUserKey);
                */
                
//encryption is bugged so let's demo without.
                out.println(str);//post current string in the output stream (to send it to the chat room)
            }//end if
            if(str.equalsIgnoreCase("/quit")){//the user is all done. do a logout.
                logout();
            }//end if
            userText.setText("");//reset text field for next string
        });//end sendButton actionlistener lambda expression
        chatText=new JTextArea();//create the object that will contain the chat
        chatText.setLineWrap(true);//words will wrap around instead of run off the side
        chatText.setWrapStyleWord(true);//wrap after a whole word and not just a single character
        chatArea=new JScrollPane();//the scrollable object that holds the entire chat
        chatArea.setViewportView(chatText);//we will always be viewing and scrolling through the chat and nothing else
        topBanner=new JPanel();//create the banner above the chat containing all the necessary utilities
        topBanner.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));//right-aligned banner
        topBanner.add(usernameLabel);//add all the objects to the top banner
        topBanner.add(usernameField);
        topBanner.add(passwordLabel);
        topBanner.add(passwordField);
        topBanner.add(loginButton);
        topBanner.add(registerButton);
        topBanner.add(exitButton);
        bottomBanner=new JPanel();//create a banner below the chat containing the input area and submit button
        bottomBanner.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 5));//right-aligned banner
        bottomBanner.add(userText);//add all the objects to the bottom banner
        bottomBanner.add(sendButton);
        gui=new JPanel();//the panel that contains all the other panels
        gui.setBorder(new EmptyBorder(1, 6, 1, 6));//look and feel
        gui.setLayout(new BorderLayout());
        gui.add(topBanner,BorderLayout.NORTH);
        gui.add(chatArea,BorderLayout.CENTER);
        gui.add(bottomBanner,BorderLayout.SOUTH);
        setContentPane(gui);//this is the panel we'll always see in the frame. it contains the other components.
    }//end Client constructor

    @Override//Note: Java didn't like my lambda expression on my button in the constructor even though it works perfectly.
    public void actionPerformed(ActionEvent ae) {
        throw new UnsupportedOperationException("Button properly configured with lambda expression inside constructor.");
    }//end override button action event
    
    /**
     * main() makes a gui(chat()) object and changes output stream to the gui(chat()) instead.
     * @param args
     */
    public static void main(String[] args) {
            EventQueue.invokeLater(() -> {
                try {
                    todaysFace=getFace();
                    Client gui = new Client();
                    gui.setLocationRelativeTo(null);
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    SwingUtilities.updateComponentTreeUI(gui);
                    //set it up to scroll to the bottom of the chat so you don't have to
                    DefaultCaret caret = (DefaultCaret) gui.chatText.getCaret();
                    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                    //actually write to the chat room. the server gets to see this, so it needs to be encrypted first.
                    System.setOut(new PrintStream(new Chat(gui.chatText)));
                    gui.setVisible(true);
                }catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
                    JOptionPane.showMessageDialog(null, "ERROR: "+e.getMessage());
                }//end try/catch
            });//end EventQueue lambda expression
    }//end main()

    /**
     * Get a random emoji from a list, for fun.
     * @return a random face emoji
     */
    private static String getFace() {
        String face;
        Random randomFace = new Random();
        int randomInteger=randomFace.nextInt(24);
        switch(randomInteger){
            case 0: face="(￢‿￢ )";break;
            case 1: face="｡ﾟ( ﾟ^∀^ﾟ)ﾟ｡";break;
            case 2: face="( ͡° ͜ʖ ͡°)";break;
            case 3: face="ɛ( ิ ͜ʖ ิ)ɜ";break;
            case 4: face="( ˘ ɜ˘) ♬♪♫";break;
            case 5: face="( ˘▽˘)っ♨";break;
            case 6: face="ʕ ᵔᴥᵔ ʔ";break;
            case 7: face="ฅ(^◕ᴥ◕^)ฅ";break;
            case 8: face="ฅ(^◔ᴥ◔^)ฅ";break;
            case 9: face="(つ ◕‿◕ )つ";break;
            case 10: face="(◕‿◕✿)";break;
            case 11: face="(⌐■_■)";break;
            case 12: face="🐔";break;
            case 13: face="(ﾉ◕ヮ◕)ﾉ*:･ﾟ✧";break;
            case 14: face="(ﾉ´ヮ`)ﾉ*. ･ﾟ";break;
            case 15: face="(￣ヘ￣)";break;
            case 16: face="(≖ ͜ʖ≖)";break;
            case 17: face="​ヾ(￣▽￣)ノ〃";break;
            case 18: face="(ﾉ≧∀≦)ﾉ";break;
            case 19: face="ヘ(￣ω￣ヘ)";break;
            case 20: face="(〜￣▽￣)〜";break;
            case 21: face="(￣▽￣)";break;
            case 22: face="∪･ｪ･∪";break;
            case 23: face="(*¯︶¯*)";break;
            default: face="｡･ﾟﾟ*(>д<)*ﾟﾟ･｡";break;
        }//end switch logic
        return face;
    }//end method getFace
    
    /**
     * Hash a password.
     * @param passwordToHash the password to hash
     * @return hashedPassword the hashed password
     */
    private String passwordHasher(String passwordToHash){
        String hashedPassword=passwordToHash;
        try{
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(passwordToHash.getBytes());
            byte[] bytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for(int i=0;i<bytes.length;i++){
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            hashedPassword = sb.toString();
        }catch(NoSuchAlgorithmException e){
            System.out.println("ERROR: "+e.getMessage());
        }
        return hashedPassword;
    }//end passwordHasher

    /**
     * Logout an online user and update the look and feel of the GUI.
     */
    private void logout() {
        loginButton.setText("LOGIN");
        usernameField.setText("");
        passwordField.setText("");
        registerButton.setVisible(true);
        usernameField.setEditable(true);
        passwordField.setEditable(true);
        repaint();
        try {//close the connection
            clientSocket.close();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "ERROR: "+e.getMessage());
        }//end try/catch
        usernameField.requestFocusInWindow();//user will want to immediately log back in with a username, so focus on the usernameField JTextField
        getRootPane().setDefaultButton(loginButton);//since the user logged out, change the default button back to the login button
    }//end logout method
    
    /**
     * Login an existing user and update the look and feel of the GUI.
     */
    private void login() throws InvalidKeySpecException, NoSuchAlgorithmException{
        loginButton.setText("LOGGING IN...");
        loginButton.setEnabled(false);
        registerButton.setVisible(false);
        exitButton.setVisible(false);
        userText.setEditable(false);
        sendButton.setEnabled(false);
        userText.requestFocusInWindow();//user will want to immediately start typing, so focus on the userText JTextField
        getRootPane().setDefaultButton(sendButton);//since user logged in, change the default button to the send chat button
        repaint();
        String login = usernameField.getText();
        String password = String.valueOf(passwordField.getPassword());
        //store in the database a hashed password instead of a real password
        String hashedPassword=passwordHasher(password);
        if(login.isEmpty() || password.isEmpty()){
            loginButton.setText("LOGIN");
            repaint();
            JOptionPane.showMessageDialog(null, "Enter Login Info!");
        }else{
            try{
                PreparedStatement ps;
                ResultSet rs;
                try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost/mucdb","root","root")) {
                    ps = con.prepareStatement("select * from users where username = ? and password = ?");
                    ps.setString(1,login);
                    ps.setString(2,hashedPassword);//store the hash instead
                    rs = ps.executeQuery();
                    if(rs.next()){
                        try {
                            clientName=login;
                            usernameField.setText(login);
                            usernameField.setEditable(false);
                            passwordField.setEditable(false);
                            clientSocket = new Socket("localhost", PORT);
                            out = new PrintWriter(clientSocket.getOutputStream(), true);
                            new Thread(new Listener()).start();
                            //tell server your name
                            out.println(clientName);
                            repaint();
                            JOptionPane.showMessageDialog(this, "Please wait patiently while your secret key is synthesized.");
                        //encryption stuff
                            //User Has A Key
                            a = new UserKeys(); //client side
                            userPublicKey = a.GetMyPublicKey();
                            byte[] rsaByteKey = userPublicKey.getEncoded(); //make encoded users key - client side
                            String rsaStringKey = Base64.getEncoder().encodeToString(rsaByteKey); //make string from encoded key - client side
                            //Pass String To Server
                            out.println("%"+rsaStringKey);//server will scan for strings that start with 3 whitespaces
                            JOptionPane.showMessageDialog(this, "Key created.");
                            loginButton.setText("LOGOUT");//we're officially logged in.
                            repaint();
                        }catch(IOException e){
                            loginButton.setText("LOGIN");
                            registerButton.setVisible(true);
                            repaint();
                            if(e.getMessage().contains("Connection refused: connect")){
                                System.out.println("Server is not online.");
                            }else{
                                System.out.println("ERROR: "+e.getMessage());
                            }//end inner else
                        }//end inner try/catch
                    }else{//end if(rs.next())
                        loginButton.setText("LOGIN");
                        registerButton.setVisible(true);
                        repaint();
                        JOptionPane.showMessageDialog(this, "Incorrect login info!");
                    }//end middle else
                }//end middle try/catch
                //close any existing connections
                ps.close();
                rs.close();
            }catch(HeadlessException | SQLException e){
                loginButton.setText("LOGIN");
                registerButton.setVisible(true);
                repaint();
                JOptionPane.showMessageDialog(this, "ERROR: "+e.getMessage());
            }//end outer try/catch
        }//end outer else
        loginButton.setEnabled(true);
        exitButton.setVisible(true);
        userText.setEditable(true);
        sendButton.setEnabled(true);
        repaint();
    }//end login method

    /**
     * Listen to input stream and write to chat.
     */
    private static class Listener implements Runnable {
        private BufferedReader in;
        private InputStreamReader inputStream;
        private String line;
        
        @Override
        public void run(){
            try{
                inputStream=new InputStreamReader(clientSocket.getInputStream());
                in=new BufferedReader(inputStream);
                while(true){
                    line=in.readLine();
                    if(line!=null && !line.isEmpty()){//avoid null pointer exception         
                        System.out.println(line);
                    }//end if
                }//end while
            }catch(IOException e){//end try
                if(!"".equals(usernameField.getText())){ //no more annoying pop-ups when you correctly close the socket.
                    if(e.getMessage().contains("Connection reset")){
                        System.out.println("Server went offline.");
                    }else{//end nested if
                        JOptionPane.showMessageDialog(null, "ERROR: "+e.getMessage());
                    }//end nested else
                }//end if
            }//end try/catch
        }//end override run
    }//end Listener class

    /**
     * GUI for user registration.
     */
    public class MucRegister extends JFrame{
        //create some GUI objects to assist in registering accounts
        JLabel usernameLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JLabel confirmPasswordLabel = new JLabel("Confirm Password");
        JPasswordField confirmPasswordField = new JPasswordField();
        JLabel emailLabel = new JLabel("Email Address");
        JTextField emailField = new JTextField();
        JButton registerButton = new JButton("Register");
        JButton backButton = new JButton("Go Back");

        /**
         * Constructs a Register GUI.
         */
        public MucRegister(){
            //GUI components
            super("Register");
            //Note the Default Close Operation: Destroy JUST this JFrame object when finished with registration.
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setBounds(0,0,230,217);//look and feel, define entire window size without calling pack()
            //top half gui
            JPanel panelTop = new JPanel();
            panelTop.setBorder(new EmptyBorder(10, 3, 1, 3));//look and feel
            panelTop.setLayout(new BoxLayout(panelTop, BoxLayout.Y_AXIS));
            panelTop.add(usernameLabel);
            panelTop.add(usernameField);
            panelTop.add(passwordLabel);
            panelTop.add(passwordField);
            panelTop.add(confirmPasswordLabel);
            panelTop.add(confirmPasswordField);
            panelTop.add(emailLabel);
            panelTop.add(emailField);
            getContentPane().add(panelTop, BoxLayout.X_AXIS);
            //bottom half gui
            JPanel panelBottom = new JPanel();
            panelBottom.setBorder(new EmptyBorder(3, 3, 4, 3));//look and feel
            panelBottom.setLayout(new BorderLayout());
            panelBottom.add(registerButton,BorderLayout.CENTER);
            panelBottom.add(backButton, BorderLayout.EAST);
            getContentPane().add(panelBottom, BorderLayout.SOUTH);
            //add action listeners to buttons
            registerButton.addActionListener((ActionEvent e)->{
                //user submits tries to make new account
                register(); //run our customized register() method
            });//end registerButton actionlistener lambda expression
            backButton.addActionListener((ActionEvent e)->{
                //go back. we're done here!
                dispose(); //use built-in dispose() method instead of setVisible(false) to prevent memory leaks
            });//end backButton actionlistener lambda expression
        } //end Register constructor

        /**
         * Write a registered user to the database.
         */
        private void register(){
            //store all the user input
            String login = usernameField.getText();
            String password = String.valueOf(passwordField.getPassword());
            String confirmPassword = String.valueOf(confirmPasswordField.getPassword());
            String email = emailField.getText();
            //allow only "real" email addresses that match this pattern
            Pattern pattern = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}");
            Matcher mat = pattern.matcher(email);
            //perform a series of checks to see if the user info is valid
            if(login.isEmpty()||password.isEmpty()||confirmPassword.isEmpty()||email.isEmpty()){
                //user didn't provide enough input
                JOptionPane.showMessageDialog(this, "Fill in all fields!");
            } else if(mat.matches()){ 
                //user typed in all fields, AND email appears to be valid format
                if(!confirmPassword.matches(password)){
                    //password check failed
                    JOptionPane.showMessageDialog(this, "Password confirmation failed. Try again!");
                } else if(confirmPassword.matches(password)){
                    //user typed in all fields, email valid, AND passwords match
                    if(login.length()>15){
                        //that username is too long!
                        JOptionPane.showMessageDialog(this, "That username is too long!");
                    } else if(login.length()<3){
                        //that username is too short!
                        JOptionPane.showMessageDialog(this, "That username is too short!");
                    }else if(password.length()>30){
                        //all these sizes are specifically defined in the database - let's control the size of user input accordingly
                        JOptionPane.showMessageDialog(this, "That password is too long!");
                    }else if(password.length()<3){
                        JOptionPane.showMessageDialog(this, "That password is too short!");
                    }else if(email.length()>50){
                        JOptionPane.showMessageDialog(this, "That email is too long!");
                    }else{
                        //check if the user already exists in the database
                        try {
                            PreparedStatement ps;
                            ResultSet rs;
                            try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost/mucdb","root","root")) {
                                ps = con.prepareStatement("select * from users where username = ?");
                                ps.setString(1, login);
                                rs = ps.executeQuery();
                                if(rs.next()){
                                    JOptionPane.showMessageDialog(this, "Username unavailable. Try again!");
                                }else{//end inner if
                                    //user input passes all checks and the new account will be created
                                    ps = con.prepareStatement("insert into users(username,email,password) values(?,?,?)");
                                    ps.setString(1,login);
                                    ps.setString(2,email);
                                    String hashedPassword=passwordHasher(password);
                                    ps.setString(3,hashedPassword); //store in the database a hashed password instead of a real one
                                    ps.executeUpdate();
                                    JOptionPane.showMessageDialog(this, "       Registered \"" + login + "\"");
                                    dispose(); //stop those pesky memory leaks! this is better than using setVisible(false)
                                }//end inner else
                            }//end inner try
                            //close any open connections
                            ps.close();
                            rs.close();
                        }catch (HeadlessException | SQLException e){//end outer try
                            JOptionPane.showMessageDialog(this, "ERROR: "+e.getMessage());
                        }//end outer try/catch
                    }//end inner else
                }//end middle else
            }else{
                //email address isn't in a recognized format
                JOptionPane.showMessageDialog(this, "Enter valid email address!");
            }//end outer else
        } //end register method
        
        /**
         * Hash a password.
         * @param passwordToHash the password to hash
         * @return hashedPassword the hashed password
         */
       private String passwordHasher(String passwordToHash){
           String hashedPassword=passwordToHash;
           try{
               MessageDigest md = MessageDigest.getInstance("MD5");
               md.update(passwordToHash.getBytes());
               byte[] bytes = md.digest();
               StringBuilder sb = new StringBuilder();
               for(int i=0;i<bytes.length;i++){
                   sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
               }
               hashedPassword = sb.toString();
           }catch(NoSuchAlgorithmException e){
               System.out.println("ERROR: "+e.getMessage());
           }
           return hashedPassword;
       }//end passwordHasher
    }//end Register class
    
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
                case '%':
                    //this character is not allowed to be typed by users. it indicates the passing of a special key object.
                    break;
                    //now % cannot be written in chat.
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
    public class MessageHandler 
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
}//end Client.java
