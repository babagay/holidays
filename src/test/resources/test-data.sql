--DROP TABLE IF EXISTS holidays;
--
--CREATE TABLE holidays (
--                          id INT AUTO_INCREMENT PRIMARY KEY,
--                          title varchar(255) UNIQUE,
--                          holidayDate TIMESTAMP UNIQUE
--);

insert into holidays (id, title, holidayDate) values (10, 'Test Knowledge day', '2023-09-15T00:00:00.000Z');
insert into holidays (id, title, holidayDate) values (20, 'Test Other day', '2025-09-16T00:00:00.000Z');