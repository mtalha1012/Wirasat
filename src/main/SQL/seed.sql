USE wirasat;

INSERT INTO asset_types (type_id, category_name) VALUES
(1, 'Cash at Bank'),
(2, 'Cash in Hand'),
(3, 'Real Estate (Property)'),
(4, 'Gold / Silver'),
(5, 'Business Equity'),
(6, 'Vehicles'),
(7, 'Investments (Stocks/Bonds)'),
(8, 'Other Assets');

INSERT INTO relation_categories (category_id, category_name) VALUES
(1, 'Spouse'),
(2, 'Primary Ascendant'),
(3, 'Primary Descendant'),
(4, 'Secondary Ascendant'),
(5, 'Secondary Descendant'),
(6, 'Sibling'),
(7, 'Extended');

INSERT INTO relation_types (relation_id, relation_name, category_id) VALUES
-- Primary Heirs (they can never be blocked!)
(1, 'Husband', 1),
(2, 'Wife', 1),
(3, 'Father', 2),
(4, 'Mother', 2),
(5, 'Son', 3),
(6, 'Daughter', 3),

-- Secondary Ascendants & Descendants (can be blocked)
(7, 'Paternal Grandfather', 4),
(8, 'Paternal Grandmother', 4),
(9, 'Maternal Grandmother', 4),
(10, 'Son''s Son (Grandson)', 5),
(11, 'Son''s Daughter (Granddaughter)', 5),

-- Siblings
(12, 'Full Brother', 6),
(13, 'Full Sister', 6),
(14, 'Consanguine (Paternal) Brother', 6),
(15, 'Consanguine (Paternal) Sister', 6),
(16, 'Uterine (Maternal) Brother', 6),
(17, 'Uterine (Maternal) Sister', 6),

-- Extended Residuaries
(18, 'Full Brother''s Son (Nephew)', 7),
(19, 'Paternal Uncle', 7),
(20, 'Paternal Uncle''s Son (Cousin)', 7);

-- FARAID BLOCKING RULES (Al-Hajb)
INSERT INTO faraid_blocking_rules (target_relation_id, blocking_relation_id) VALUES
-- Grandparents are blocked by Parents
(7, 3),
(8, 4),
(9, 4),

-- Grandchildren are blocked by Son
(10, 5),
(11, 5),

-- Siblings are blocked by Son, Father, or Grandson
(12, 5),  -- Full Brother blocked by Son
(12, 3),  -- Full Brother blocked by Father
(12, 10), -- Full Brother blocked by Son's Son
(13, 5),  -- Full Sister blocked by Son
(13, 3),  -- Full Sister blocked by Father
(13, 10), -- Full Sister blocked by Son's Son

-- Consanguine Siblings are blocked by Full Brother, Son, Father etc.
(14, 12), -- Consanguine Brother blocked by Full Brother
(14, 5),  -- Consanguine Brother blocked by Son
(14, 3),  -- Consanguine Brother blocked by Father
(15, 12), -- Consanguine Sister blocked by Full Brother
(15, 5),  -- Consanguine Sister blocked by Son
(15, 3),  -- Consanguine Sister blocked by Father

-- Uterine Siblings are blocked by Ascendants & Descendants
(16, 5),  -- Uterine Brother blocked by Son
(16, 6),  -- Uterine Brother blocked by Daughter
(16, 3),  -- Uterine Brother blocked by Father
(16, 7),  -- Uterine Brother blocked by Paternal Grandfather
(17, 5),  -- Uterine Sister blocked by Son
(17, 6),  -- Uterine Sister blocked by Daughter
(17, 3),  -- Uterine Sister blocked by Father
(17, 7),  -- Uterine Sister blocked by Paternal Grandfather

-- Extended relatives blocked by closer relations
(18, 12), -- Nephew blocked by Full Brother
(19, 12), -- Uncle blocked by Full Brother
(19, 3),  -- Uncle blocked by Father
(20, 19); -- Cousin blocked by Uncle

INSERT INTO condition_types (condition_id, condition_name, description) VALUES
(1, 'NO_CHILD', 'When the deceased has no children or grandchildren'),
(2, 'WITH_CHILD', 'When the deceased has children or grandchildren'),
(3, 'WITH_CHILD_OR_SIBLING', 'When the deceased has children or two or more siblings'),
(4, 'ONLY_ONE', 'When there is only one person of this relation'),
(5, 'MULTIPLE', 'When there are two or more persons of this relation');

-- SHARE RULES (Faraid Fractional Shares)
INSERT INTO share_rules (relation_id, numerator, denominator, condition_type) VALUES
-- Husband
(1, 1, 2, 1),        -- 1/2 NO_CHILD
(1, 1, 4, 2),        -- 1/4 WITH_CHILD

-- Wife
(2, 1, 4, 1),        -- 1/4 NO_CHILD
(2, 1, 8, 2),        -- 1/8 WITH_CHILD

-- Father
(3, 1, 6, 2),        -- 1/6 WITH_CHILD. If no children, he is residuary.

-- Mother
(4, 1, 6, 3),        -- 1/6 WITH_CHILD_OR_SIBLING
(4, 1, 3, 1),        -- 1/3 NO_CHILD

-- Daughter (when there is NO son, otherwise Asabah)
(6, 1, 2, 4),        -- 1/2 ONLY_ONE
(6, 2, 3, 5);        -- 2/3 MULTIPLE

INSERT INTO beneficiary_types (type_id, type_name) VALUES
(1, 'Individual'),
(2, 'Charity/NGO'),
(3, 'Mosque'),
(4, 'Educational Institute'),
(5, 'Hospital'),
(6, 'Other');
