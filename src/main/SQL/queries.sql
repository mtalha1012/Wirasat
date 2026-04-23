USE wirasat;

CREATE OR REPLACE VIEW latest_valuations AS
SELECT v.asset_id, v.amount, v.valuation_date
FROM valuation_history v
INNER JOIN (
    SELECT asset_id, MAX(valuation_date) AS max_date
    FROM   valuation_history
    GROUP  BY asset_id
) latest ON v.asset_id = latest.asset_id
        AND v.valuation_date = latest.max_date;


DELIMITER //

CREATE PROCEDURE ComputeNetEstate(
    IN  p_deceased_id       INT,
    OUT p_total_assets      DECIMAL(15,2),
    OUT p_total_liabilities DECIMAL(15,2),
    OUT p_executed_wasiyat  DECIMAL(15,2),
    OUT p_net_estate        DECIMAL(15,2)
)
BEGIN
    DECLARE v_raw_wasiyat DECIMAL(15,2) DEFAULT 0;
    DECLARE v_max_wasiyat DECIMAL(15,2);

    SELECT COALESCE(SUM(lv.amount), 0)
    INTO   p_total_assets
    FROM   assets a
    JOIN   latest_valuations lv ON lv.asset_id = a.asset_id
    WHERE  a.owner_id = p_deceased_id
      AND  a.is_shareable = TRUE;

    SELECT COALESCE(SUM(amount), 0)
    INTO   p_total_liabilities
    FROM   liabilities
    WHERE  deceased_id = p_deceased_id;

    SELECT COALESCE(SUM(amount), 0)
    INTO   v_raw_wasiyat
    FROM   wasiyat
    WHERE  deceased_id = p_deceased_id;

    SET v_max_wasiyat      = (p_total_assets - p_total_liabilities) / 3;
    SET p_executed_wasiyat = LEAST(v_raw_wasiyat, v_max_wasiyat);
    SET p_net_estate       = p_total_assets - p_total_liabilities - p_executed_wasiyat;
END //

CREATE PROCEDURE GetEstateState(IN p_deceased_id INT)
BEGIN
    DECLARE v_assets    DECIMAL(15,2);
    DECLARE v_liab      DECIMAL(15,2);
    DECLARE v_wasiyat   DECIMAL(15,2);
    DECLARE v_net       DECIMAL(15,2);

    CALL ComputeNetEstate(p_deceased_id, v_assets, v_liab, v_wasiyat, v_net);

    SELECT
        v_assets                  AS Total_Assets,
        v_liab                    AS Total_Liabilities,
        v_wasiyat                 AS Executed_Wasiyat,
        (v_assets - v_liab) / 3  AS Max_Allowed_Wasiyat,
        v_net                     AS Net_Distributable_Estate;

    SELECT
        dh.heir_id,
        fm.name,
        fm.gender,
        rt.relation_name,
        rt.category
    FROM   deceased_heirs dh
    JOIN   family_members fm ON fm.member_id   = dh.heir_id
    JOIN   relation_types rt ON rt.relation_id = dh.relation_id
    WHERE  dh.deceased_id = p_deceased_id;
END //


CREATE PROCEDURE calculate_distribution(IN p_deceased_id INT)
BEGIN
    DECLARE v_assets    DECIMAL(15,2);
    DECLARE v_liab      DECIMAL(15,2);
    DECLARE v_wasiyat   DECIMAL(15,2);
    DECLARE v_net       DECIMAL(15,2);
    DECLARE v_run_id    INT;

    CALL ComputeNetEstate(p_deceased_id, v_assets, v_liab, v_wasiyat, v_net);

    INSERT INTO calculation_runs (deceased_id, net_estate)
    VALUES (p_deceased_id, v_net);
    SET v_run_id = LAST_INSERT_ID();

    INSERT INTO distribution_logs (run_id, heir_id, share_fraction, share_amount)
    SELECT
        v_run_id,
        dh.heir_id,
        sr.numerator / sr.denominator,
        sr.numerator / sr.denominator * v_net
    FROM  deceased_heirs dh
    JOIN  share_rules sr ON sr.relation_id = dh.relation_id
    WHERE dh.deceased_id = p_deceased_id
      AND dh.relation_id NOT IN (
              SELECT fbr.target_relation_id
              FROM   faraid_blocking_rules fbr
              WHERE  fbr.blocking_relation_id IN (
                         SELECT relation_id
                         FROM   deceased_heirs
                         WHERE  deceased_id = p_deceased_id
                     )
          );

    SELECT CONCAT(
        'Distribution complete. Run ID: ', v_run_id,
        ' | Net Estate: PKR ', FORMAT(v_net, 2)
    ) AS Result;
END //

DELIMITER ;
