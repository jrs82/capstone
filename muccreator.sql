drop database if exists mucdb;
create database mucdb;
use mucdb;
create table users(userid int not null auto_increment, username varchar(15) not null, password varchar(100) not null, email varchar(50) not null, primary key(userid));
insert into users(username, password, email)
 values('test', 'test', 'test@test.com');
 insert into users(username, password, email)
   values('josh','josh', 'josh@test.com');
 insert into users(username, password, email)
 values('matt','matt', 'matt@test.com');
 insert into users(username, password, email)
   values('johnny','johnny', 'johnny@test.com');
	insert into users(username, password, email)
 values('adam','adam', 'adam@test.com');
 select * from users;