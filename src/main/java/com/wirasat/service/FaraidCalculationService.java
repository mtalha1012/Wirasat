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

    public static class HeirCandidate {
        public FamilyMember member;
        public String relationName;
        public int relationId;
        public double fractionAssigned = 0.0;
        public boolean isAsabah = false;

        public HeirCandidate(FamilyMember member, String relationName, int relationId) {
            this.member = member;
            this.relationName = relationName;
            this.relationId = relationId;
        }
    }

    //Overloaded method for backward compatibility if Wasiyat and Liabilities are not provided.
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

    //main calculation engine evaluating the entire Faraid mathematical structure.
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

        // Step 1: Net Estate Computation (Assets - Liabilities - Wasiyat)
        double grossEstate = calculateTotalAssetValue(assets);
        double totalLiabilities = calculateTotalLiabilities(liabilities);
        double totalWasiyat = calculateTotalWasiyat(wasiyats, grossEstate - totalLiabilities);
        
        double netEstate = grossEstate - totalLiabilities - totalWasiyat;
        if (netEstate <= 0) {
            run.setNetEstate(BigDecimal.ZERO);
            return run;
        }
        run.setNetEstate(BigDecimal.valueOf(netEstate));

        // Step 2: Establish the alive candidates
        List<HeirCandidate> livingHeirs = determineLivingHeirs(deceased, allFamilyMembers, heirMappings, relationTypes);

        // Step 3: Apply Al-Hajb (Blocking Rules)
        List<HeirCandidate> eligibleHeirs = applyBlockingRules(livingHeirs, blockingRules);

        // Step 4: Determine conditions (e.g. are there children?)
        boolean hasChild = eligibleHeirs.stream().anyMatch(h -> 
            h.relationName.toLowerCase().contains("son") || 
            h.relationName.toLowerCase().contains("daughter"));
            
        boolean hasMaleChild = eligibleHeirs.stream().anyMatch(h -> 
            h.relationName.toLowerCase().contains("son") && !h.relationName.toLowerCase().contains("grand"));

        long siblingCount = eligibleHeirs.stream().filter(h -> 
            h.relationName.toLowerCase().contains("brother") || 
            h.relationName.toLowerCase().contains("sister")).count();

        // Step 5: Assign Zawil Furuz (Fixed preset shares) based on family state
        double totalFractions = assignFixedShares(eligibleHeirs, shareRules, hasChild, hasMaleChild, siblingCount);

        // Step 6: Resolve Awl (Over-subscription) or Asabah (Residuaries) and Radd (Return)
        if (totalFractions > 1.0) {
            applyAwl(eligibleHeirs, totalFractions);
        } else if (totalFractions < 1.0) {
            double remainder = 1.0 - totalFractions;
            boolean hasAsabah = distributeToAsabah(eligibleHeirs, remainder);
            
            if (!hasAsabah && remainder > 0.0001) {
                applyRadd(eligibleHeirs, totalFractions);
            }
        }

        // Step 7: Finalize monetary allocations
        List<AssetAllocation> allocations = processMonetaryAllocations(eligibleHeirs, netEstate);
        
        return run;
    }

    private double calculateTotalAssetValue(List<Asset> assets) {
        return assets.stream()
            .mapToDouble(a -> a.getCurrentValue() != null ? a.getCurrentValue().doubleValue() : 0.0)
            .sum();
    }

    private double calculateTotalLiabilities(List<Liability> liabilities) {
        if (liabilities == null) return 0.0;
        return liabilities.stream()
            .mapToDouble(l -> l.getAmount() != null ? l.getAmount().doubleValue() : 0.0)
            .sum();
    }

    private double calculateTotalWasiyat(List<Wasiyat> wasiyats, double availableEstate) {
        if (wasiyats == null || availableEstate <= 0) return 0.0;
        double sum = wasiyats.stream()
            .mapToDouble(w -> w.getAmount() != null ? w.getAmount().doubleValue() : 0.0)
            .sum();
        
        // Wasiyat cannot exceed 1/3 of the estate after debts
        double maxAllowable = availableEstate / 3.0;
        return Math.min(sum, maxAllowable);
    }

    private List<HeirCandidate> determineLivingHeirs(FamilyMember deceased, 
                                                     List<FamilyMember> allFamilyMembers,
                                                     List<DeceasedHeir> heirMappings,
                                                     List<RelationType> relationTypes) {
        List<HeirCandidate> livingHeirs = new ArrayList<>();
        
        for (DeceasedHeir mapping : heirMappings) {
            if (mapping.getDeceasedId() == deceased.getMemberId()) {
                FamilyMember member = allFamilyMembers.stream()
                        .filter(m -> m.getMemberId() == mapping.getHeirId())
                        .findFirst().orElse(null);
                        
                RelationType relation = relationTypes.stream()
                        .filter(r -> r.getRelationId() == mapping.getRelationId())
                        .findFirst().orElse(null);
                        
                if (member != null && relation != null) {
                    // Heir must be alive at the time of the deceased's passing
                    if (member.getDateOfDeath() == null || 
                       (deceased.getDateOfDeath() != null && member.getDateOfDeath().after(deceased.getDateOfDeath()))) {
                        livingHeirs.add(new HeirCandidate(member, relation.getRelationName(), relation.getRelationId()));
                    }
                }
            }
        }
        return livingHeirs;
    }

    private List<HeirCandidate> applyBlockingRules(List<HeirCandidate> candidates, List<FaraidBlockingRule> blockingRules) {
        List<Integer> presentRelationIds = candidates.stream()
                .map(c -> c.relationId)
                .collect(Collectors.toList());

        // A relation is blocked if any of its defined blockers exist in the current candidate pool
        List<Integer> blockedRelationIds = blockingRules.stream()
                .filter(rule -> presentRelationIds.contains(rule.getBlockingRelationId()))
                .map(FaraidBlockingRule::getTargetRelationId)
                .collect(Collectors.toList());

        return candidates.stream()
                .filter(c -> !blockedRelationIds.contains(c.relationId))
                .collect(Collectors.toList());
    }

    private double assignFixedShares(List<HeirCandidate> heirs, List<ShareRule> rules, 
                                     boolean hasChild, boolean hasMaleChild, long siblingCount) {
        double totalFractions = 0.0;

        // Group heirs by relation to split shares evenly among identical relations (e.g. 3 daughters sharing 2/3)
        var groupedHeirs = heirs.stream().collect(Collectors.groupingBy(h -> h.relationId));

        for (var entry : groupedHeirs.entrySet()) {
            int relId = entry.getKey();
            List<HeirCandidate> group = entry.getValue();
            HeirCandidate rep = group.get(0);
            
            // Determine condition logic contextually
            String conditionMatch = determineConditionState(rep.relationName, hasChild, hasMaleChild, siblingCount, group.size());

            ShareRule appliedRule = rules.stream()
                    .filter(r -> r.getRelationId() == relId && 
                                (r.getConditionType() == null || r.getConditionType().equalsIgnoreCase(conditionMatch) || r.getConditionType().equalsIgnoreCase("DEFAULT")))
                    .findFirst().orElse(null);

            if (appliedRule != null && appliedRule.getDenominator() > 0) {
                double totalGroupShare = (double) appliedRule.getNumerator() / appliedRule.getDenominator();
                
                // Special Ta'seeb (Residuary) overrides: A daughter with a son becomes Asabah, losing fixed fraction
                if (rep.relationName.toLowerCase().contains("daughter") && hasMaleChild) {
                    group.forEach(h -> h.isAsabah = true);
                } else if (rep.relationName.toLowerCase().contains("son")) {
                    group.forEach(h -> h.isAsabah = true); // Sons are primarily Asabah
                } else {
                    double individualShare = totalGroupShare / group.size();
                    for (HeirCandidate h : group) {
                        h.fractionAssigned = individualShare;
                        totalFractions += individualShare;
                    }
                }
            } else {
                // If no fixed rule applies gracefully, assume they are residuary candidates
                group.forEach(h -> h.isAsabah = true);
            }
        }
        return totalFractions;
    }

    private String determineConditionState(String relation, boolean hasChild, boolean hasMaleChild, long siblingCount, int groupSize) {
        String lowerRel = relation.toLowerCase();
        
        if (lowerRel.contains("wife") || lowerRel.contains("husband")) {
            return hasChild ? "WITH_CHILD" : "NO_CHILD";
        }
        if (lowerRel.contains("mother")) {
            if (hasChild || siblingCount > 1) return "WITH_CHILD_OR_SIBLINGS";
            return "DEFAULT";
        }
        if (lowerRel.contains("daughter")) {
            return groupSize > 1 ? "MULTIPLE" : "SINGLE";
        }
        if (lowerRel.contains("sister")) {
            return groupSize > 1 ? "MULTIPLE" : "SINGLE";
        }
        return "DEFAULT";
    }

    private void applyAwl(List<HeirCandidate> heirs, double totalFractions) {
        // Proportional reduction by shifting the base denominator
        for (HeirCandidate h : heirs) {
            if (h.fractionAssigned > 0) {
                h.fractionAssigned = h.fractionAssigned / totalFractions;
            }
        }
    }

    private boolean distributeToAsabah(List<HeirCandidate> heirs, double remainder) {
        List<HeirCandidate> asabahList = heirs.stream()
                .filter(h -> h.isAsabah)
                .collect(Collectors.toList());

        if (asabahList.isEmpty()) return false;

        // Calculate relative weights. Male = 2, Female = 1.
        double totalWeight = 0;
        for (HeirCandidate a : asabahList) {
            if (isMaleRelation(a.relationName)) totalWeight += 2.0;
            else totalWeight += 1.0;
        }

        for (HeirCandidate a : asabahList) {
            double weight = isMaleRelation(a.relationName) ? 2.0 : 1.0;
            a.fractionAssigned = remainder * (weight / totalWeight);
        }
        
        return true;
    }

    private void applyRadd(List<HeirCandidate> heirs, double currentTotal) {
        // Exclude spouses from Radd in classic Faraid
        List<HeirCandidate> raddEligible = heirs.stream()
                .filter(h -> h.fractionAssigned > 0 && 
                             !h.relationName.toLowerCase().contains("wife") && 
                             !h.relationName.toLowerCase().contains("husband"))
                .collect(Collectors.toList());

        if (raddEligible.isEmpty()) return;

        double nonSpouseTotal = raddEligible.stream().mapToDouble(h -> h.fractionAssigned).sum();
        double remainingToRedistribute = 1.0 - currentTotal;

        for (HeirCandidate h : raddEligible) {
            double proportion = h.fractionAssigned / nonSpouseTotal;
            h.fractionAssigned += (remainingToRedistribute * proportion);
        }
    }

    private boolean isMaleRelation(String relation) {
        String r = relation.toLowerCase();
        return r.contains("son") || r.contains("brother") || r.contains("father") || 
               r.contains("husband") || r.contains("uncle") || r.contains("nephew");
    }

    private List<AssetAllocation> processMonetaryAllocations(List<HeirCandidate> heirs, double netValue) {
        List<AssetAllocation> allocations = new ArrayList<>();
        
        for (HeirCandidate h : heirs) {
            if (h.fractionAssigned <= 0) continue;

            double amount = netValue * h.fractionAssigned;
            double percentage = h.fractionAssigned * 100.0;

            AssetAllocation allocation = new AssetAllocation();
            allocation.setHeirId(h.member.getMemberId());
            
            BigDecimal bdPct = BigDecimal.valueOf(percentage).setScale(4, RoundingMode.HALF_UP);
            BigDecimal bdAmt = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
            
            allocation.setAllocatedPercentage(bdPct);
            allocation.setAllocatedValue(bdAmt);
            allocation.setFinalized(false);
            
            allocations.add(allocation);
        }
        return allocations;
    }
}
