package com.wirasat.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wirasat.model.Asset;
import com.wirasat.model.AssetAllocation;
import com.wirasat.model.CalculationRun;
import com.wirasat.model.DeceasedHeir;
import com.wirasat.model.FamilyMember;
import com.wirasat.model.RelationType;
import com.wirasat.model.ShareRule;

public class FaraidCalculationService {


    public static class HeirCandidate {
        public FamilyMember member;
        public String relationName;
        public int relationId;

        public HeirCandidate(FamilyMember member, String relationName, int relationId) {
            this.member = member;
            this.relationName = relationName;
            this.relationId = relationId;
        }
    }

    /*
     * @param deceased         The family member who passed away.
     * @param allFamilyMembers A list of all known family members.
     * @param heirMappings     The specific DeceasedHeir mapping table connecting the deceased to their heirs.
     * @param relationTypes    The catalog of relation types (Son, Wife, Brother, etc.).
     * @param blockingRules    The catalog of Faraid blocking rules from the database.
     * @param shareRules       The catalog of fractional share rules from the database.
     * @param assets           The total assets left by the deceased.
     * @return                 A CalculationRun containing the final asset allocations.
     */
    public CalculationRun calculateFaraid(FamilyMember deceased, 
                                          List<FamilyMember> allFamilyMembers, 
                                          List<DeceasedHeir> heirMappings,
                                          List<RelationType> relationTypes,
                                          List<com.wirasat.model.FaraidBlockingRule> blockingRules,
                                          List<ShareRule> shareRules,
                                          List<Asset> assets) {
        CalculationRun run = new CalculationRun();
        run.setRunDate(new Date());
        run.setDeceasedId(deceased.getMemberId());

        double totalAssetValue = calculateTotalAssetValue(assets);

        List<HeirCandidate> livingHeirs = determineLivingHeirs(deceased, allFamilyMembers, heirMappings, relationTypes);

        // Apply Blocking Rules (Al-Hajb) using database rules
        List<HeirCandidate> eligibleHeirs = applyBlockingRules(livingHeirs, blockingRules);

        // Determine basic shares (Zawil Furuz) using database fractional rules
        List<AssetAllocation> allocations = calculateShares(eligibleHeirs, shareRules, totalAssetValue);

        run.setNetEstate(java.math.BigDecimal.valueOf(totalAssetValue));
        
        return run;
    }

    private double calculateTotalAssetValue(List<Asset> assets) {
        // Compute total monetary value
        return 0.0;
    }

    private List<HeirCandidate> determineLivingHeirs(FamilyMember deceased, 
                                                     List<FamilyMember> allFamilyMembers,
                                                     List<DeceasedHeir> heirMappings,
                                                     List<RelationType> relationTypes) {
        List<HeirCandidate> livingHeirs = new ArrayList<>();
        
        for (DeceasedHeir mapping : heirMappings) {
            if (mapping.getDeceasedId() == deceased.getMemberId()) {
                // Find the family member
                FamilyMember member = allFamilyMembers.stream()
                        .filter(m -> m.getMemberId() == mapping.getHeirId())
                        .findFirst().orElse(null);
                        
                // Find the relation type to get name and ID
                RelationType relation = relationTypes.stream()
                        .filter(r -> r.getRelationId() == mapping.getRelationId())
                        .findFirst().orElse(null);
                        
                String relationName = relation != null ? relation.getRelationName() : "Unknown";
                int relationId = relation != null ? relation.getRelationId() : -1;

                if (member != null) {
                    if (member.getDateOfDeath() == null || 
                       (deceased.getDateOfDeath() != null && member.getDateOfDeath().after(deceased.getDateOfDeath()))) {
                        livingHeirs.add(new HeirCandidate(member, relationName, relationId));
                    }
                }
            }
        }
        return livingHeirs;
    }

    private List<HeirCandidate> applyBlockingRules(List<HeirCandidate> candidates, List<com.wirasat.model.FaraidBlockingRule> blockingRules) {
        // Collect all relation IDs present among the living candidates
        List<Integer> presentRelationIds = new ArrayList<>();
        for (HeirCandidate c : candidates) {
            presentRelationIds.add(c.relationId);
        }

        // Determine which relation IDs are blocked by ANY of the currently present candidates
        List<Integer> blockedRelationIds = new ArrayList<>();
        for (com.wirasat.model.FaraidBlockingRule rule : blockingRules) {
            if (presentRelationIds.contains(rule.getBlockingRelationId())) {
                blockedRelationIds.add(rule.getTargetRelationId());
            }
        }

        // Filter out candidates whose relation ID is in the blocked list
        List<HeirCandidate> eligible = new ArrayList<>();
        for (HeirCandidate candidate : candidates) {
            if (!blockedRelationIds.contains(candidate.relationId)) {
                eligible.add(candidate);
            } else {
                System.out.println("Blocked Heir: " + candidate.member.getName() + " (" + candidate.relationName + ")");
            }
        }

        return eligible;
    }

    private List<AssetAllocation> calculateShares(List<HeirCandidate> eligibleHeirs, List<ShareRule> shareRules, double totalValue) {
        List<AssetAllocation> allocations = new ArrayList<>();
        
        // As a basic example of applying rules:
        for (HeirCandidate heir : eligibleHeirs) {
            // Find a basic matching share rule for this relation type
            ShareRule matchedRule = shareRules.stream()
                .filter(rule -> rule.getRelationId() == heir.relationId)
                .findFirst()
                .orElse(null);

            double shareAmount = 0.0;
            double percentage = 0.0;

            if (matchedRule != null && matchedRule.getDenominator() > 0) {
                percentage = ((double) matchedRule.getNumerator() / matchedRule.getDenominator()) * 100.0;
                shareAmount = totalValue * (percentage / 100.0);
                System.out.println(heir.member.getName() + " (" + heir.relationName + ") receives " + 
                                   matchedRule.getNumerator() + "/" + matchedRule.getDenominator() + 
                                   " share: " + shareAmount);
            } else {
                // E.g., for Asabahs (residuary) where there is no fixed fraction, handle separately.
                System.out.println(heir.member.getName() + " (" + heir.relationName + ") is marked as a residuary (Asabah) or missing rule.");
            }

            AssetAllocation allocation = new AssetAllocation();
            allocation.setHeirId(heir.member.getMemberId());
            allocation.setAllocatedPercentage(java.math.BigDecimal.valueOf(percentage));
            allocation.setAllocatedValue(java.math.BigDecimal.valueOf(shareAmount));
            allocation.setFinalized(false);
            
            allocations.add(allocation);
        }
        
        // TODO: Distribute the remaining estate total to Asabah (residuary heirs).
        return allocations;
    }
}
