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
-- Primary Heirs
(1, 'Husband', 1),
(2, 'Wife', 1),
(3, 'Father', 2),
(4, 'Mother', 2),
(5, 'Son', 3),
(6, 'Daughter', 3),
-- Secondary Ascendants & Descendants
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

-- FARAID BLOCKING RULES
INSERT INTO faraid_blocking_rules (target_relation_id, blocking_relation_id) VALUES
-- Grandparents
(7, 3),
(8, 4),
(9, 4),
-- Grandchildren
(10, 5),
(11, 5),
-- Siblings
(12, 5),
(12, 3),
(12, 10),
(13, 5),
(13, 3),
(13, 10),
-- Consanguine Siblings
(14, 12),
(14, 5),
(14, 3),
(15, 12),
(15, 5),
(15, 3),
-- Uterine Siblings
(16, 5),
(16, 6),
(16, 3),
(16, 7),
(17, 5),
(17, 6),
(17, 3),
(17, 7),
-- Extended Relatives
(18, 12),
(19, 12),
(19, 3),
(20, 19);

INSERT INTO condition_types (condition_id, condition_name, description) VALUES
(1, 'NO_CHILD', 'When the deceased has no children or grandchildren'),
(2, 'WITH_CHILD', 'When the deceased has children or grandchildren'),
(3, 'WITH_CHILD_OR_SIBLING', 'When the deceased has children or two or more siblings'),
(4, 'ONLY_ONE', 'When there is only one person of this relation'),
(5, 'MULTIPLE', 'When there are two or more persons of this relation');

-- SHARE RULES
INSERT INTO share_rules (relation_id, numerator, denominator, condition_type) VALUES
-- Husband
(1, 1, 2, 1),
(1, 1, 4, 2),
-- Wife
(2, 1, 4, 1),
(2, 1, 8, 2),
-- Father
(3, 1, 6, 2),
-- Mother
(4, 1, 6, 3),
(4, 1, 3, 1),
-- Daughter
(6, 1, 2, 4),
(6, 2, 3, 5);

INSERT INTO beneficiary_types (type_id, type_name) VALUES
(1, 'Individual'),
(2, 'Charity/NGO'),
(3, 'Mosque'),
(4, 'Educational Institute'),
(5, 'Hospital'),
(6, 'Other');

INSERT INTO family_members (member_id, cnic, name, date_of_birth, gender, date_of_death, father_id, mother_id) VALUES
(1, '35202-1111111-1', 'Tariq Mahmood', '1960-05-15', 'M', '2026-01-01', NULL, NULL),
(2, '35202-2222222-2', 'Ayesha Tariq', '1965-08-20', 'F', NULL, NULL, NULL),
(3, '35202-3333333-3', 'Ali Tariq', '1990-10-10', 'M', NULL, 1, 2),
(4, '35202-4444444-4', 'Fatima Tariq', '1995-12-05', 'F', NULL, 1, 2),
(5, '35202-5555555-5', 'Usman Mahmood', '1962-07-12', 'M', NULL, NULL, NULL);

INSERT INTO deceased_heirs (mapping_id, deceased_id, heir_id, relation_id) VALUES
(1, 1, 2, 2),
(2, 1, 3, 5),
(3, 1, 4, 6),
(4, 1, 5, 12);

INSERT INTO assets (asset_id, asset_name, type_id, owner_id, is_shareable, value) VALUES
(1, 'DHA Phase 5 House', 3, 1, TRUE, 35000000.00),
(2, 'Meezan Bank Savings Account', 1, 1, TRUE, 2500000.00),
(3, 'Toyota Corolla 2022', 6, 1, TRUE, 5500000.00);

INSERT INTO valuation_history (valuation_history_id, asset_id, valuation_date, amount) VALUES
(1, 1, '2026-01-05', 35000000.00),
(2, 2, '2026-01-05', 2500000.00),
(3, 3, '2026-01-05', 5500000.00);

INSERT INTO liabilities (liability_id, details, amount, deceased_id) VALUES
(1, 'Pending Hospital Bills', 150000.00, 1),
(2, 'Personal Loan from Brother', 500000.00, 1);

INSERT INTO beneficiaries (beneficiary_id, beneficiary_name, beneficiary_type, member_id) VALUES
(1, 'Edhi Foundation', 2, NULL);

INSERT INTO wasiyat (will_id, deceased_id, beneficiary_id, amount) VALUES
(1, 1, 1, 1000000.00);

INSERT INTO calculation_runs (run_id, deceased_id, run_date, net_estate) VALUES
(1, 1, '2026-02-01', 41350000.00);

INSERT INTO distribution_logs (log_id, share_fraction, share_amount, heir_id, run_id) VALUES
(1, 0.12500000, 5168750.00, 2, 1),
(2, 0.58333333, 24120833.33, 3, 1),
(3, 0.29166667, 12060416.67, 4, 1);

INSERT INTO asset_allocations (allocation_id, asset_id, heir_id, allocated_percentage, is_finalized) VALUES
(1, 1, 2, 12.50, FALSE),
(2, 1, 3, 58.33, FALSE),
(3, 1, 4, 29.17, FALSE);
