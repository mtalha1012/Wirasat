package com.wirasat.service;

import com.wirasat.model.*;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Central in-memory data store mirroring the MySQL schema.
 * All seed data matches seed.sql exactly.
 */
public class MockDataService {
    private static MockDataService instance;

    private List<FamilyMember> familyMembers = new ArrayList<>();
    private List<Asset> assets = new ArrayList<>();
    private List<AssetType> assetTypes = new ArrayList<>();
    private List<Liability> liabilities = new ArrayList<>();
    private List<Wasiyat> wasiyats = new ArrayList<>();
    private List<DeceasedHeir> deceasedHeirs = new ArrayList<>();
    private List<RelationType> relationTypes = new ArrayList<>();
    private List<FaraidBlockingRule> blockingRules = new ArrayList<>();
    private List<ShareRule> shareRules = new ArrayList<>();

    private int nextMemberId = 100;
    private int nextAssetId = 100;
    private int nextLiabilityId = 100;
    private int nextWasiyatId = 100;
    private int nextMappingId = 100;

    /** Explicit principal deceased — never auto-detected */
    private int principalDeceasedId = 1;

    private MockDataService() { initializeData(); }

    public static MockDataService getInstance() {
        if (instance == null) instance = new MockDataService();
        return instance;
    }

    private void initializeData() {
        // --- asset_types (seed.sql) ---
        assetTypes.add(new AssetType(1, "Cash at Bank"));
        assetTypes.add(new AssetType(2, "Cash in Hand"));
        assetTypes.add(new AssetType(3, "Real Estate (Property)"));
        assetTypes.add(new AssetType(4, "Gold / Silver"));
        assetTypes.add(new AssetType(5, "Business Equity"));
        assetTypes.add(new AssetType(6, "Vehicles"));
        assetTypes.add(new AssetType(7, "Investments (Stocks/Bonds)"));
        assetTypes.add(new AssetType(8, "Other Assets"));

        // --- relation_types (seed.sql lines 14-41 exact) ---
        relationTypes.add(new RelationType(1, "Husband", "Spouse"));
        relationTypes.add(new RelationType(2, "Wife", "Spouse"));
        relationTypes.add(new RelationType(3, "Father", "Primary Ascendant"));
        relationTypes.add(new RelationType(4, "Mother", "Primary Ascendant"));
        relationTypes.add(new RelationType(5, "Son", "Primary Descendant"));
        relationTypes.add(new RelationType(6, "Daughter", "Primary Descendant"));
        relationTypes.add(new RelationType(7, "Paternal Grandfather", "Secondary Ascendant"));
        relationTypes.add(new RelationType(8, "Paternal Grandmother", "Secondary Ascendant"));
        relationTypes.add(new RelationType(9, "Maternal Grandmother", "Secondary Ascendant"));
        relationTypes.add(new RelationType(10, "Son's Son (Grandson)", "Secondary Descendant"));
        relationTypes.add(new RelationType(11, "Son's Daughter (Granddaughter)", "Secondary Descendant"));
        relationTypes.add(new RelationType(12, "Full Brother", "Sibling"));
        relationTypes.add(new RelationType(13, "Full Sister", "Sibling"));
        relationTypes.add(new RelationType(14, "Consanguine (Paternal) Brother", "Sibling"));
        relationTypes.add(new RelationType(15, "Consanguine (Paternal) Sister", "Sibling"));
        relationTypes.add(new RelationType(16, "Uterine (Maternal) Brother", "Sibling"));
        relationTypes.add(new RelationType(17, "Uterine (Maternal) Sister", "Sibling"));
        relationTypes.add(new RelationType(18, "Full Brother's Son (Nephew)", "Extended"));
        relationTypes.add(new RelationType(19, "Paternal Uncle", "Extended"));
        relationTypes.add(new RelationType(20, "Paternal Uncle's Son (Cousin)", "Extended"));

        // --- faraid_blocking_rules (seed.sql lines 44-84, all 24) ---
        int b = 1;
        blockingRules.add(new FaraidBlockingRule(b++, 7, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 8, 4));
        blockingRules.add(new FaraidBlockingRule(b++, 9, 4));
        blockingRules.add(new FaraidBlockingRule(b++, 10, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 11, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 12, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 12, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 12, 10));
        blockingRules.add(new FaraidBlockingRule(b++, 13, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 13, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 13, 10));
        blockingRules.add(new FaraidBlockingRule(b++, 14, 12));
        blockingRules.add(new FaraidBlockingRule(b++, 14, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 14, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 15, 12));
        blockingRules.add(new FaraidBlockingRule(b++, 15, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 15, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 16, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 16, 6));
        blockingRules.add(new FaraidBlockingRule(b++, 16, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 16, 7));
        blockingRules.add(new FaraidBlockingRule(b++, 17, 5));
        blockingRules.add(new FaraidBlockingRule(b++, 17, 6));
        blockingRules.add(new FaraidBlockingRule(b++, 17, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 17, 7));
        blockingRules.add(new FaraidBlockingRule(b++, 18, 12));
        blockingRules.add(new FaraidBlockingRule(b++, 19, 12));
        blockingRules.add(new FaraidBlockingRule(b++, 19, 3));
        blockingRules.add(new FaraidBlockingRule(b++, 20, 19));

        // --- share_rules (seed.sql lines 87-105 exact) ---
        int s = 1;
        shareRules.add(new ShareRule(s++, 1, 1, 2, "NO_CHILD"));
        shareRules.add(new ShareRule(s++, 1, 1, 4, "WITH_CHILD"));
        shareRules.add(new ShareRule(s++, 2, 1, 4, "NO_CHILD"));
        shareRules.add(new ShareRule(s++, 2, 1, 8, "WITH_CHILD"));
        shareRules.add(new ShareRule(s++, 3, 1, 6, "WITH_CHILD"));
        shareRules.add(new ShareRule(s++, 4, 1, 6, "WITH_CHILD_OR_SIBLING"));
        shareRules.add(new ShareRule(s++, 4, 1, 3, "NO_CHILD"));
        shareRules.add(new ShareRule(s++, 6, 1, 2, "ONLY_ONE"));
        shareRules.add(new ShareRule(s++, 6, 2, 3, "MULTIPLE"));

        // --- family_members (demo) ---
        // Principal deceased — DOD = Jan 1 2025 (fixed, past date)
        Calendar decDOD = Calendar.getInstance();
        decDOD.set(2025, Calendar.JANUARY, 1);
        Calendar decDOB = Calendar.getInstance();
        decDOB.set(1960, Calendar.MARCH, 15);

        FamilyMember deceased = new FamilyMember(1, "12345-1234567-1", "Muhammad Aslam Khan",
                decDOB.getTime(), 'M', 65, decDOD.getTime(), null, null);

        Calendar sonDOB = Calendar.getInstance(); sonDOB.set(1990, Calendar.JUNE, 10);
        FamilyMember son = new FamilyMember(2, "12345-1234567-2", "Ali Khan",
                sonDOB.getTime(), 'M', 35, null, 1, null);

        Calendar dauDOB = Calendar.getInstance(); dauDOB.set(1993, Calendar.SEPTEMBER, 20);
        FamilyMember daughter = new FamilyMember(3, "12345-1234567-3", "Aisha Khan",
                dauDOB.getTime(), 'F', 32, null, 1, null);

        Calendar wifeDOB = Calendar.getInstance(); wifeDOB.set(1965, Calendar.DECEMBER, 5);
        FamilyMember wife = new FamilyMember(4, "12345-1234567-4", "Fatima Bibi",
                wifeDOB.getTime(), 'F', 60, null, null, null);

        familyMembers.add(deceased);
        familyMembers.add(son);
        familyMembers.add(daughter);
        familyMembers.add(wife);
        principalDeceasedId = 1;

        // --- deceased_heirs ---
        deceasedHeirs.add(new DeceasedHeir(1, 1, 2, 5));  // Son (rel=5)
        deceasedHeirs.add(new DeceasedHeir(2, 1, 3, 6));  // Daughter (rel=6)
        deceasedHeirs.add(new DeceasedHeir(3, 1, 4, 2));  // Wife (rel=2)

        // --- assets (owned by deceased, type IDs match seed.sql) ---
        Asset a1 = new Asset(1, "Main House — Gulberg, Lahore", 3, 1, false);
        a1.setValue(new BigDecimal("25000000"));
        Asset a2 = new Asset(2, "DHA Phase 5 Plot", 3, 1, false);
        a2.setValue(new BigDecimal("19000000"));
        Asset a3 = new Asset(3, "Allied Bank Savings Account", 1, 1, true);
        a3.setValue(new BigDecimal("5200000"));
        assets.add(a1); assets.add(a2); assets.add(a3);

        // --- liabilities ---
        liabilities.add(new Liability(1, "Bank Loan — HBL", new BigDecimal("1000000"), 1));

        // --- wasiyat ---
        wasiyats.add(new Wasiyat(1, 1, 0, new BigDecimal("500000")));
    }

    // === PRINCIPAL DECEASED ===
    public int getPrincipalDeceasedId() { return principalDeceasedId; }
    public void setPrincipalDeceasedId(int id) { this.principalDeceasedId = id; }

    public FamilyMember getDeceased() {
        return familyMembers.stream().filter(m -> m.getMemberId() == principalDeceasedId).findFirst().orElse(null);
    }

    // === SCOPED QUERIES (mirrors queries.sql WHERE deceased_id = ?) ===
    public List<Asset> getDeceasedAssets() {
        return assets.stream().filter(a -> a.getOwnerId() == principalDeceasedId).collect(Collectors.toList());
    }
    public List<Liability> getDeceasedLiabilities() {
        return liabilities.stream().filter(l -> l.getDeceasedId() == principalDeceasedId).collect(Collectors.toList());
    }
    public List<Wasiyat> getDeceasedWasiyats() {
        return wasiyats.stream().filter(w -> w.getDeceasedId() == principalDeceasedId).collect(Collectors.toList());
    }
    public List<DeceasedHeir> getDeceasedHeirMappings() {
        return deceasedHeirs.stream().filter(dh -> dh.getDeceasedId() == principalDeceasedId).collect(Collectors.toList());
    }

    // === RAW GETTERS ===
    public List<FamilyMember> getFamilyMembers() { return familyMembers; }
    public List<Asset> getAssets() { return assets; }
    public List<AssetType> getAssetTypes() { return assetTypes; }
    public List<Liability> getLiabilities() { return liabilities; }
    public List<Wasiyat> getWasiyats() { return wasiyats; }
    public List<DeceasedHeir> getDeceasedHeirs() { return deceasedHeirs; }
    public List<RelationType> getRelationTypes() { return relationTypes; }
    public List<FaraidBlockingRule> getBlockingRules() { return blockingRules; }
    public List<ShareRule> getShareRules() { return shareRules; }

    // === MUTATORS ===
    public void addFamilyMember(FamilyMember m) { m.setMemberId(nextMemberId++); familyMembers.add(m); }
    public void removeFamilyMember(FamilyMember m) {
        familyMembers.remove(m);
        deceasedHeirs.removeIf(dh -> dh.getHeirId() == m.getMemberId() || dh.getDeceasedId() == m.getMemberId());
    }
    public void addAsset(Asset a) { a.setAssetId(nextAssetId++); assets.add(a); }
    public void removeAsset(Asset a) { assets.remove(a); }
    public void addLiability(Liability l) { l.setLiabilityId(nextLiabilityId++); liabilities.add(l); }
    public void removeLiability(Liability l) { liabilities.remove(l); }
    public void addWasiyat(Wasiyat w) { w.setWillId(nextWasiyatId++); wasiyats.add(w); }
    public void removeWasiyat(Wasiyat w) { wasiyats.remove(w); }

    public void addDeceasedHeir(DeceasedHeir dh) {
        if (dh.getDeceasedId() == dh.getHeirId()) return; // CHECK constraint
        boolean exists = deceasedHeirs.stream().anyMatch(
                e -> e.getDeceasedId() == dh.getDeceasedId() && e.getHeirId() == dh.getHeirId());
        if (exists) return; // UNIQUE constraint
        dh.setMappingId(nextMappingId++);
        deceasedHeirs.add(dh);
    }

    // === LOOKUPS ===
    public FamilyMember getMemberById(int id) {
        return familyMembers.stream().filter(m -> m.getMemberId() == id).findFirst().orElse(null);
    }
    public RelationType getRelationTypeById(int id) {
        return relationTypes.stream().filter(r -> r.getRelationId() == id).findFirst().orElse(null);
    }
    public AssetType getAssetTypeById(int id) {
        return assetTypes.stream().filter(t -> t.getTypeId() == id).findFirst().orElse(null);
    }
}
