CREATE DATABASE wirasat;
USE wirasat;

CREATE TABLE asset_types(
	type_id INT AUTO_INCREMENT,
    category_name VARCHAR(30),
    
    CONSTRAINT asset_types_pk
		PRIMARY KEY(type_id)
    );
    
CREATE TABLE family_members(
    member_id INT AUTO_INCREMENT,
    cnic VARCHAR(15),
    name VARCHAR(20),
    date_of_birth DATETIME,
    gender CHAR(1),
    age INT AS (
        TIMESTAMPDIFF(YEAR, date_of_birth, CURDATE())
        ) VIRTUAL,
    status VARCHAR(8),
    date_of_death DATETIME,

    CONSTRAINT family_members_age
        CHECK (gender IN ('M', 'F')),
    CONSTRAINT family_members_pk
        PRIMARY KEY(member_id)

    )