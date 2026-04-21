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

INSERT INTO relation_types (relation_id, relation_name, category) VALUES
-- Primary Heirs (they can never be blocked!)
(1, 'Husband', 'Spouse'),
(2, 'Wife', 'Spouse'),
(3, 'Father', 'Primary Ascendant'),
(4, 'Mother', 'Primary Ascendant'),
(5, 'Son', 'Primary Descendant'),
(6, 'Daughter', 'Primary Descendant'),

-- Secondary Ascendants & Descendants (can be blocked)
(7, 'Paternal Grandfather', 'Secondary Ascendant'),
(8, 'Paternal Grandmother', 'Secondary Ascendant'),
(9, 'Maternal Grandmother', 'Secondary Ascendant'),
(10, 'Son''s Son (Grandson)', 'Secondary Descendant'),
(11, 'Son''s Daughter (Granddaughter)', 'Secondary Descendant'),

-- Siblings
(12, 'Full Brother', 'Sibling'),
(13, 'Full Sister', 'Sibling'),
(14, 'Consanguine (Paternal) Brother', 'Sibling'),
(15, 'Consanguine (Paternal) Sister', 'Sibling'),
(16, 'Uterine (Maternal) Brother', 'Sibling'),
(17, 'Uterine (Maternal) Sister', 'Sibling'),

-- Extended Residuaries
(18, 'Full Brother''s Son (Nephew)', 'Extended'),
(19, 'Paternal Uncle', 'Extended'),
(20, 'Paternal Uncle''s Son (Cousin)', 'Extended');

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

