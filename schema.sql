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
    );
    
CREATE TABLE liabilities(
	liability_id INT AUTO_INCREMENT,
    amount DECIMAL(15,2),
    deceased_id INT,
    
    CONSTRAINT liabilities_pk
		PRIMARY KEY(liability_id),
    CONSTRAINT liabilities_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES
        family_members(member_id)
	);
    
CREATE TABLE wasiyat(
    will_id INT AUTO_INCREMENT,
    deceased_id INT,
    beneficiary_id INT,
    
    CONSTRAINT wasiyat_pk
		PRIMARY KEY(will_id),
	CONSTRAINT wasiyat_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES
        family_members(member_id),
	CONSTRAINT wasiyat_beneficiary_fk
		FOREIGN KEY(beneficiary_id) REFERENCES
        family_members(member_id)
    );
    
CREATE TABLE assets(
	asset_id INT AUTO_INCREMENT,
    asset_name VARCHAR(30),
    type_id INT,
    owner_id INT,
    
    CONSTRAINT asstes_pk
		PRIMARY KEY(asset_id),
	CONSTRAINT assets_type_fk
		FOREIGN KEY(type_id) REFERENCES
        asset_types(type_id),
	CONSTRAINT assets_owner_fk
		FOREIGN KEY(owner_id) REFERENCES
        family_members(member_id)
	);
    
CREATE TABLE valuation_history(
	asset_id INT,
    valuation_date DATETIME,
    amount DECIMAL(12, 2),
    
    CONSTRAINT valuation_history_pk
		PRIMARY KEY(asset_id, valuation_date),
	CONSTRAINT valuation_history_asset_fk
		FOREIGN KEY(asset_id) REFERENCES
        assets(asset_id)
	);
    
CREATE TABLE distribution_logs(
	log_id INT AUTO_INCREMENT,
    share_fraction REAL,
    share_amount DECIMAL(12, 2),
    run_date DATETIME,
    deceased_id INt,
    heir_id INt,
    
    CONSTRAINT distribution_logs_pk
		PRIMARY KEY(log_id),
	CONSTRAINT distribution_logs_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES
        family_members(member_id),
	CONSTRAINT distribution_logs_heir_fk
		FOREIGN KEY(heir_id) REFERENCES
        family_members(member_id)
	);
        
    