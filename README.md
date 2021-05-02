# capstone

The database needs to be set up in MySQL beforehand. It is hardcoded in the software.
All the names need to match the software.
Database name: 
mucdb
Table name: 
users
Create table statement: 
create table users(userid int not null auto_increment, username varchar(15) not null, password varchar(100) not null, email varchar(50) not null, primary key(userid));

The final release of Server.java and Client.java have a package "capstone" on the first line. It may need to be commented out in order to compile outside of Netbeans.

Admin launches Server.java and presses start button.

User launches Client.java and either logs in or registers an account.
Login and Register functionality performs SQL queries with PreparedStatement

It's hardcoded with the table name.

Once user logs in, he gets encryption keys made then he can chat with others in the client until the server is placed offline.
