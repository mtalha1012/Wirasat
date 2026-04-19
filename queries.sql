USE wirasat;

CREATE OR REPLACE VIEW latest_valuations AS
SELECT v.asset_id, v.amount, v.valuation_date
FROM valuation_history v
INNER JOIN (
    SELECT asset_id, MAX(valuation_date) as max_date 
    FROM valuation_history 
    GROUP BY asset_id
) latest ON v.asset_id = latest.asset_id AND v.valuation_date = latest.max_date;

DELIMITER //

CREATE PROCEDURE GetEstateState(IN target_deceased_id INT)
BEGIN
    DECLARE total_assets DECIMAL(15,2) DEFAULT 0;
    DECLARE total_liabilities DECIMAL(15,2) DEFAULT 0;
    DECLARE total_wasiyat DECIMAL(15,2) DEFAULT 0;
    DECLARE max_wasiyat DECIMAL(15,2) DEFAULT 0;
    DECLARE net_estate DECIMAL(15,2) DEFAULT 0;

    SELECT COALESCE(SUM(lv.amount), 0) INTO total_assets
    FROM assets a
    JOIN latest_valuations lv ON a.asset_id = lv.asset_id
    WHERE a.owner_id = target_deceased_id;

    SELECT COALESCE(SUM(amount), 0) INTO total_liabilities
    FROM liabilities 
    WHERE deceased_id = target_deceased_id;

    SELECT COALESCE(SUM(amount), 0) INTO total_wasiyat
    FROM wasiyat 
    WHERE deceased_id = target_deceased_id;

    SET max_wasiyat = (total_assets - total_liabilities) / 3;
    IF total_wasiyat > max_wasiyat THEN
        SET total_wasiyat = max_wasiyat;
    END IF;

    SET net_estate = total_assets - total_liabilities - total_wasiyat;

    SELECT 
        total_assets AS Total_Assets,
        total_liabilities AS Total_Liabilities,
        total_wasiyat AS Executed_Wasiyat,
        max_wasiyat AS Max_Allowed_Wasiyat,
        net_estate AS Net_Distributable_Estate;

    SELECT 
        dh.heir_id, 
        fm.name, 
        fm.gender, 
        rt.relation_name,
        rt.category
    FROM deceased_heirs dh
    JOIN family_members fm ON dh.heir_id = fm.member_id
    JOIN relation_types rt ON dh.relation_id = rt.relation_id
    WHERE dh.deceased_id = target_deceased_id;

END //
DELIMITER ;
