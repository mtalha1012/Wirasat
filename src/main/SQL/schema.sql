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
    cnic VARCHAR(15) UNIQUE,
    name VARCHAR(100) NOT NULL,
    date_of_birth DATETIME NOT NULL,
    gender CHAR(1) NOT NULL,
    age INT AS (
		TIMESTAMPDIFF(
			YEAR, date_of_birth, 
			IFNULL(date_of_death, CURDATE())
			)
		) VIRTUAL,
    date_of_death DATETIME,
    father_id INT NULL,
    mother_id INT NULL,

    CONSTRAINT family_members_gender_chk
        CHECK (gender IN ('M', 'F')),
    CONSTRAINT family_members_pk
        PRIMARY KEY(member_id),
	CONSTRAINT father_id_fk
		FOREIGN KEY(father_id) REFERENCES
        family_members(member_id),
	CONSTRAINT mother_id_fk
		FOREIGN KEY(mother_id) REFERENCES
        family_members(member_id)
    );
    
CREATE TABLE relation_types(
	relation_id INT AUTO_INCREMENT,
    relation_name VARCHAR(50) UNIQUE,
    category VARCHAR(50),
    
    CONSTRAINT relation_types_pk
		PRIMARY KEY(relation_id)
	);
    
CREATE TABLE deceased_heirs(
	mapping_id INT AUTO_INCREMENT,
    deceased_id INT,
    heir_id INT,
    relation_id INT,
    
    CONSTRAINT deceased_heirs_pk
		PRIMARY KEY(mapping_id),
	CONSTRAINT deceased_heirs_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES
        family_members(member_id),
	CONSTRAINT deceased_heirs_heir_fk
		FOREIGN KEY(heir_id) REFERENCES
        family_members(member_id),
	CONSTRAINT deceased_heirs_relation_fk
		FOREIGN KEY(relation_id) REFERENCES
        relation_types(relation_id),
	CONSTRAINT deceased_heirs_unique
		UNIQUE(deceased_id, heir_id),
	CONSTRAINT deceased_not_heir_chk
		CHECK (deceased_id != heir_id)
	);
    
CREATE TABLE liabilities(
	liability_id INT AUTO_INCREMENT,
    details VARCHAR(255),
    amount DECIMAL(15,2),
    deceased_id INT,
    
    CONSTRAINT liabilities_pk
		PRIMARY KEY(liability_id),
    CONSTRAINT liabilities_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES
        family_members(member_id)
	);
    
CREATE TABLE beneficiaries(
	beneficiary_id INT AUTO_INCREMENT,
    beneficiary_name VARCHAR(100),
	beneficiary_type VARCHAR(30),
    member_id INT NULL,
    
    CONSTRAINT beneficiary_pk
		PRIMARY KEY(beneficiary_id),
	CONSTRAINT beneficiary_member_id
		FOREIGN KEY(member_id) REFERENCES
        family_members(member_id)
	);
    
CREATE TABLE wasiyat(
    will_id INT AUTO_INCREMENT,
    deceased_id INT,
    beneficiary_id INT,
    amount DECIMAL(15, 2),
    
    CONSTRAINT wasiyat_pk
		PRIMARY KEY(will_id),
	CONSTRAINT wasiyat_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES
        family_members(member_id),
	CONSTRAINT wasiyat_beneficiary_fk
		FOREIGN KEY(beneficiary_id) REFERENCES
        beneficiaries(beneficiary_id)
    );
    
CREATE TABLE assets(
	asset_id INT AUTO_INCREMENT,
    asset_name VARCHAR(100),
    type_id INT,
    owner_id INT,
    is_shareable BOOLEAN DEFAULT TRUE,
    
    CONSTRAINT assets_pk
		PRIMARY KEY(asset_id),
	CONSTRAINT assets_type_fk
		FOREIGN KEY(type_id) REFERENCES
        asset_types(type_id),
	CONSTRAINT assets_owner_fk
		FOREIGN KEY(owner_id) REFERENCES
        family_members(member_id)
	);
    
CREATE TABLE valuation_history(
	valuation_history_id INT AUTO_INCREMENT,
	asset_id INT,
    valuation_date DATETIME,
    amount DECIMAL(12, 2),
    
    CONSTRAINT valuation_history_pk
		PRIMARY KEY(valuation_history_id),
	CONSTRAINT valuation_history_asset_fk
		FOREIGN KEY(asset_id) REFERENCES
        assets(asset_id)
	);
    
CREATE TABLE asset_allocations(
	allocation_id INT AUTO_INCREMENT,
    asset_id INT,
    heir_id INT,
    run_id INT,
    allocated_percentage DECIMAL(5, 2),
    allocated_value DECIMAL(15, 2),
    is_finalized BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT percentage_chk
		CHECK(allocated_percentage BETWEEN 0 and 100),
    CONSTRAINT asset_allocations_pk
		PRIMARY KEY(allocation_id),
	CONSTRAINT asset_allocations_asset_fk
		FOREIGN KEY(asset_id) REFERENCES
        assets(asset_id),
	CONSTRAINT asset_allocations_heir_fk
		FOREIGN KEY(heir_id) REFERENCES
        family_members(member_id),
	CONSTRAINT asset_allocations_run_fk
		FOREIGN KEY(run_id) REFERENCES
        calculation_runs(run_id)
	);
    
CREATE TABLE calculation_runs(
	run_id INT AUTO_INCREMENT,
    deceased_id INT NOT NULL,
    run_date DATETIME NOT NULL DEFAULT NOW(),
    net_estate DECIMAL(15, 2),
    
    CONSTRAINT calculation_runs_pk
		PRIMARY KEY(run_id),
	CONSTRAINT calculation_runs_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES 
        family_members(member_id)
	);
    
CREATE TABLE distribution_logs(
	log_id INT AUTO_INCREMENT,
    share_fraction DECIMAL(10, 8),
    share_amount DECIMAL(12, 2),
    deceased_id INT,
    heir_id INT,
    run_id INT,
    
    CONSTRAINT distribution_logs_pk
		PRIMARY KEY(log_id),
	CONSTRAINT distribution_logs_deceased_fk
		FOREIGN KEY(deceased_id) REFERENCES
        family_members(member_id),
	CONSTRAINT distribution_logs_heir_fk
		FOREIGN KEY(heir_id) REFERENCES
        family_members(member_id),
	CONSTRAINT calculation_run_fk
		FOREIGN KEY(run_id) REFERENCES
        calculation_runs(run_id)
	);

CREATE TABLE faraid_blocking_rules (
    rule_id INT AUTO_INCREMENT,
    target_relation_id INT,
    blocking_relation_id INT,
    
    CONSTRAINT faraid_blocking_rules_pk 
        PRIMARY KEY(rule_id),
    CONSTRAINT target_relation_fk 
        FOREIGN KEY(target_relation_id) REFERENCES relation_types(relation_id),
    CONSTRAINT fk_blocking_relation 
        FOREIGN KEY(blocking_relation_id) REFERENCES relation_types(relation_id),
    CONSTRAINT uiq_target_blocking 
        UNIQUE(target_relation_id, blocking_relation_id)
);

