package com.wirasat.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.wirasat.model.Asset;
import com.wirasat.model.AssetAllocation;
import com.wirasat.model.CalculationRun;
import com.wirasat.model.DeceasedHeir;
import com.wirasat.model.FamilyMember;
import com.wirasat.model.FaraidBlockingRule;
import com.wirasat.model.Liability;
import com.wirasat.model.RelationType;
import com.wirasat.model.ShareRule;
import com.wirasat.model.Wasiyat;


public class FaraidCalculationService {

    // Database mapped Relation IDs from seed.sql
    private static final int REL_HUSBAND = 1;
    private static final int REL_WIFE = 2;
    private static final int REL_FATHER = 3;
    private static final int REL_MOTHER = 4;
    private static final int REL_SON = 5;
    private static final int REL_DAUGHTER = 6;
    private static final int REL_PAT_GRANDFATHER = 7;
    private static final int REL_PAT_GRANDMOTHER = 8;
    private static final int REL_MAT_GRANDMOTHER = 9;
    private static final int REL_GRANDSON = 10;
    private static final int REL_GRANDDAUGHTER = 11;
    private static final int REL_FULL_BROTHER = 12;
    private static final int REL_FULL_SISTER = 13;
    private static final int REL_CONS_BROTHER = 14;
    private static final int REL_CONS_SISTER = 15;
    private static final int REL_UTERINE_BROTHER = 16;
    private static final int REL_UTERINE_SISTER = 17;

    public static class HeirCandidate {
        public FamilyMember member;
        public RelationType relation;
        public double fractionAssigned = 0.0;
        public boolean isAsabah = false;
        public int asabahClass = 99; // Lower is higher priority

        public HeirCandidate(FamilyMember member, RelationType relation) {
            this.member = member;
            this.relation = relation;
        }
    }

    public CalculationRun calculateFaraid(FamilyMember deceased, 
                                          List<FamilyMember> allFamilyMembers, 
                                          List<DeceasedHeir> heirMappings,
                                          List<RelationType> relationTypes,
                                          List<FaraidBlockingRule> blockingRules,
                                          List<ShareRule> shareRules,
                                          List<Asset> assets) {
        return calculateFaraid(deceased, allFamilyMembers, heirMappings, relationTypes, 
                               blockingRules, shareRules, assets, new ArrayList<>(), new ArrayList<>());
    }

    public CalculationRun calculateFaraid(FamilyMember deceased, 
                                          List<FamilyMember> allFamilyMembers, 
                                          List<DeceasedHeir> heirMappings,
                                          List<RelationType> relationTypes,
                                          List<FaraidBlockingRule> blockingRules,
                                          List<ShareRule> shareRules,
                                          List<Asset> assets,
                                          List<Liability> liabilities,
                                          List<Wasiyat> wasiyats) {

        CalculationRun run = new CalculationRun();
        run.setRunDate(new Date());
        run.setDeceasedId(deceased.getMemberId());

        double grossEstate = calculateTotalAssetValue(assets);
        double totalLiabilities = calculateTotalLiabilities(liabilities);
        double totalWasiyat = calculateTotalWasiyat(wasiyats, grossEstate - totalLiabilities);
        
        double netEstate = grossEstate - totalLiabilities - totalWasiyat;
        if (netEstate <= 0) {
            run.setNetEstate(BigDecimal.ZERO);
            return run;
        }
        run.setNetEstate(BigDecimal.valueOf(netEstate));

        List<HeirCandidate> livingHeirs = determineLivingHeirs(deceased, allFamilyMembers, heirMappings, relationTypes);
        List<HeirCandidate> eligibleHeirs = applyBlockingRules(livingHeirs, blockingRules);

        // Derive contextual facts dynamically from Database columns ("gender" and "category")
        boolean hasChildOrGrandchild = eligibleHeirs.stream()
            .anyMatch(h -> h.relation.getCategory() != null && h.relation.getCategory().contains("Descendant"));
            
        boolean hasMaleDescendant = eligibleHeirs.stream()
            .anyMatch(h -> h.relation.getCategory() != null && h.relation.getCategory().contains("Descendant") && 
                           String.valueOf(h.member.getGender()).equalsIgnoreCase("M"));

        long siblingCount = eligibleHeirs.stream()
            .filter(h -> h.relation.getCategory() != null && h.relation.getCategory().contains("Sibling")).count();

        double totalFractions = assignFixedShares(eligibleHeirs, shareRules, hasChildOrGrandchild, hasMaleDescendant, siblingCount);

        if (totalFractions > 1.0) {
            applyAwl(eligibleHeirs, totalFractions);
        } else if (totalFractions < 1.0) {
            double remainder = 1.0 - totalFractions;
            boolean hasAsabah = distributeToAsabah(eligibleHeirs, remainder);
            
            if (!hasAsabah && remainder > 0.0001) {
                applyRadd(eligibleHeirs, totalFractions);
            }
        }

        // Dual capacity fallback: Father or Grandfather takes the rest if daughters maxed out shares but left remainder
        if (!eligibleHeirs.stream().anyMatch(h -> h.isAsabah) && !hasMaleDescendant && hasChildOrGrandchild) {
            double finalSum = eligibleHeirs.stream().mapToDouble(h -> h.fractionAssigned).sum();
            if (finalSum < 1.0) {
                HeirCandidate dualCapacity = eligibleHeirs.stream()
                    .filter(h -> h.relation.getRelationId() == REL_FATHER || h.relation.getRelationId() == REL_PAT_GRANDFATHER)
                    .findFirst().orElse(null);
                if (dualCapacity != null) {
                    dualCapacity.fractionAssigned += (1.0 - finalSum);
                }
            }
        }

        List<AssetAllocation> allocations = processMonetaryAllocations(eligibleHeirs, netEstate);
        
        return run;
    }

    private double calculateTotalAssetValue(List<Asset> assets) {
        return assets.stream().mapToDouble(a -> a.getValue() != null ? a.getValue().doubleValue() : 0.0).sum();
    }

    private double calculateTotalLiabilities(List<Liability> liabilities) {
        if (liabilities == null) return 0.0;
        return liabilities.stream().mapToDouble(l -> l.getAmount() != null ? l.getAmount().doubleValue() : 0.0).sum();
    }

    private double calculateTotalWasiyat(List<Wasiyat> wasiyats, double availableEstate) {
        if (wasiyats == null || availableEstate <= 0) return 0.0;
        double sum = wasiyats.stream().mapToDouble(w -> w.getAmount() != null ? w.getAmount().doubleValue() : 0.0).sum();
        return Math.min(sum, availableEstate / 3.0);
    }

    private List<HeirCandidate> determineLivingHeirs(FamilyMember deceased, List<FamilyMember> allFamilyMembers, 
                                                     List<DeceasedHeir> heirMappings, List<RelationType> relationTypes) {
        List<HeirCandidate> livingHeirs = new ArrayList<>();
        for (DeceasedHeir mapping : heirMappings) {
            if (mapping.getDeceasedId() == deceased.getMemberId()) {
                FamilyMember member = allFamilyMembers.stream().filter(m -> m.getMemberId() == mapping.getHeirId()).findFirst().orElse(null);
                RelationType relation = relationTypes.stream().filter(r -> r.getRelationId() == mapping.getRelationId()).findFirst().orElse(null);
                
                if (member != null && relation != null && (member.getDateOfDeath() == null || (deceased.getDateOfDeath() != null && member.getDateOfDeath().after(deceased.getDateOfDeath())))) {
                    livingHeirs.add(new HeirCandidate(member, relation));
                }
            }
        }
        return livingHeirs;
    }

    private List<HeirCandidate> applyBlockingRules(List<HeirCandidate> candidates, List<FaraidBlockingRule> blockingRules) {
        List<Integer> presentRelationIds = candidates.stream().map(c -> c.relation.getRelationId()).collect(Collectors.toList());
        List<Integer> blockedRelationIds = blockingRules.stream()
                .filter(rule -> presentRelationIds.contains(rule.getBlockingRelationId()))
                .map(FaraidBlockingRule::getTargetRelationId).collect(Collectors.toList());

        return candidates.stream().filter(c -> !blockedRelationIds.contains(c.relation.getRelationId())).collect(Collectors.toList());
    }

    private double assignFixedShares(List<HeirCandidate> heirs, List<ShareRule> rules, 
                                     boolean hasChild, boolean hasMaleDescendant, long siblingCount) {
        double totalFractions = 0.0;
        var groupedHeirs = heirs.stream().collect(Collectors.groupingBy(h -> h.relation.getRelationId()));

        for (var entry : groupedHeirs.entrySet()) {
            int relId = entry.getKey();
            List<HeirCandidate> group = entry.getValue();
            RelationType relation = group.get(0).relation;
            
            String conditionMatch = determineConditionState(relation, hasChild, siblingCount, group.size());

            ShareRule appliedRule = rules.stream()
                    .filter(r -> r.getRelationId() == relId && 
                                (r.getConditionType() == null || r.getConditionType().equalsIgnoreCase(conditionMatch) || r.getConditionType().equalsIgnoreCase("DEFAULT")))
                    .findFirst().orElse(null);

            if (appliedRule != null && appliedRule.getDenominator() > 0) {
                // Female Ta'seeb checks (e.g. Daughters become Asabah if Son exists)
                // Dynamically check if a male of the EXACT same category is present
                boolean maleCounterpartExists = heirs.stream().anyMatch(h -> 
                    String.valueOf(h.member.getGender()).equalsIgnoreCase("M") && 
                    h.relation.getCategory() != null &&
                    h.relation.getCategory().equals(relation.getCategory()) && 
                    h.relation.getRelationId() != relation.getRelationId() // Not themselves
                );

                if (String.valueOf(group.get(0).member.getGender()).equalsIgnoreCase("F") && maleCounterpartExists) {
                    group.forEach(h -> setupAsabah(h, getAsabahClass(relation.getCategory())));
                } else {
                    double totalGroupShare = (double) appliedRule.getNumerator() / appliedRule.getDenominator();
                    double individualShare = totalGroupShare / group.size();
                    for (HeirCandidate h : group) {
                        h.fractionAssigned = individualShare;
                        totalFractions += individualShare;
                    }
                }
            } else {
                // If no fractional rule matched in the database, they naturally default to Asabah
                for (HeirCandidate h : group) {
                    setupAsabah(h, getAsabahClass(relation.getCategory()));
                }
            }
        }
        return totalFractions;
    }

    private int getAsabahClass(String category) {
        if (category == null) return 99;
        if (category.contains("Descendant")) return 1;
        if (category.contains("Ascendant")) return 2;
        if (category.contains("Sibling")) return 3;
        if (category.contains("Extended")) return 4;
        return 99;
    }

    private void setupAsabah(HeirCandidate h, int asabahClass) {
        h.isAsabah = true;
        h.asabahClass = asabahClass;
    }

    private String determineConditionState(RelationType relation, boolean hasChild, long siblingCount, int groupSize) {
        int relId = relation.getRelationId();

        if (relId == REL_HUSBAND || relId == REL_WIFE) return hasChild ? "WITH_CHILD" : "NO_CHILD";
        if (relId == REL_MOTHER) return (hasChild || siblingCount > 1) ? "WITH_CHILD_OR_SIBLING" : "NO_CHILD";
        if (relId == REL_FATHER) return hasChild ? "WITH_CHILD" : "DEFAULT";
        
        // Single vs Multiple conditions for females receiving fractions
        if (relId == REL_DAUGHTER || relId == REL_GRANDDAUGHTER || 
            relId == REL_FULL_SISTER || relId == REL_CONS_SISTER || relId == REL_UTERINE_SISTER) {
            return groupSize > 1 ? "MULTIPLE" : "ONLY_ONE";
        }
        
        return "DEFAULT";
    }

    private void applyAwl(List<HeirCandidate> heirs, double totalFractions) {
        for (HeirCandidate h : heirs) {
            if (h.fractionAssigned > 0) h.fractionAssigned /= totalFractions;
        }
    }

    private boolean distributeToAsabah(List<HeirCandidate> heirs, double remainder) {
        List<HeirCandidate> allAsabah = heirs.stream().filter(h -> h.isAsabah).collect(Collectors.toList());
        if (allAsabah.isEmpty()) return false;

        int topClass = allAsabah.stream().mapToInt(h -> h.asabahClass).min().orElse(99);
        List<HeirCandidate> activeAsabah = allAsabah.stream().filter(h -> h.asabahClass == topClass).collect(Collectors.toList());

        // Uterine siblings exception - check specific ID mapped to Uterine category
        boolean isUterine = activeAsabah.stream().anyMatch(h -> 
            h.relation.getRelationId() == REL_UTERINE_BROTHER || h.relation.getRelationId() == REL_UTERINE_SISTER);

        double totalWeight = 0;
        for (HeirCandidate a : activeAsabah) {
            if (isUterine) {
                totalWeight += 1.0; // 1:1 sharing exception
            } else {
                totalWeight += String.valueOf(a.member.getGender()).equalsIgnoreCase("M") ? 2.0 : 1.0; // 2:1 mapping
            }
        }

        for (HeirCandidate a : activeAsabah) {
            double weight = 1.0;
            if (!isUterine && String.valueOf(a.member.getGender()).equalsIgnoreCase("M")) weight = 2.0;
            a.fractionAssigned += remainder * (weight / totalWeight); 
        }
        return true;
    }

    private void applyRadd(List<HeirCandidate> heirs, double currentTotal) {
        List<HeirCandidate> raddEligible = heirs.stream()
                .filter(h -> h.fractionAssigned > 0 && (h.relation.getCategory() == null || !h.relation.getCategory().equalsIgnoreCase("Spouse")))
                .collect(Collectors.toList());

        if (raddEligible.isEmpty()) return;

        double nonSpouseTotal = raddEligible.stream().mapToDouble(h -> h.fractionAssigned).sum();
        double remainingToRedistribute = 1.0 - currentTotal;

        for (HeirCandidate h : raddEligible) {
            double proportion = h.fractionAssigned / nonSpouseTotal;
            h.fractionAssigned += (remainingToRedistribute * proportion);
        }
    }

    private List<AssetAllocation> processMonetaryAllocations(List<HeirCandidate> heirs, double netValue) {
        List<AssetAllocation> allocations = new ArrayList<>();
        for (HeirCandidate h : heirs) {
            if (h.fractionAssigned <= 0) continue;

            AssetAllocation allocation = new AssetAllocation();
            allocation.setHeirId(h.member.getMemberId());
            allocation.setAllocatedPercentage(BigDecimal.valueOf(h.fractionAssigned * 100.0).setScale(4, RoundingMode.HALF_UP));
            allocation.setAllocatedValue(BigDecimal.valueOf(netValue * h.fractionAssigned).setScale(2, RoundingMode.HALF_UP));
            allocation.setFinalized(false);
            allocations.add(allocation);
        }
        return allocations;
    }
}
