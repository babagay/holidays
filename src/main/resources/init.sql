DROP TABLE IF EXISTS holidays;

CREATE TABLE holidays (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          title varchar(255) UNIQUE,
                          holidayDate TIMESTAMP UNIQUE
);