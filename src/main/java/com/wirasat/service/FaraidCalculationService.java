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

public class FaraidCalculationService {

    /**
     * A helper DTO to carry a member alongside their verified Faraid relationship to the deceased.
     */
    public static class HeirCandidate {
        public FamilyMember member;
        public String relationName;

        public HeirCandidate(FamilyMember member, String relationName) {
            this.member = member;
            this.relationName = relationName;
        }
    }

    /*
     * @param deceased         The family member who passed away.
     * @param allFamilyMembers A list of all known family members.
     * @param heirMappings     The specific DeceasedHeir mapping table connecting the deceased to their heirs.
     * @param relationTypes    The catalog of relation types (Son, Wife, Brother, etc.).
     * @param assets           The total assets left by the deceased.
     * @return                 A CalculationRun containing the final asset allocations.
     */
    public CalculationRun calculateFaraid(FamilyMember deceased, 
                                          List<FamilyMember> allFamilyMembers, 
                                          List<DeceasedHeir> heirMappings,
                                          List<RelationType> relationTypes,
                                          List<Asset> assets) {
        CalculationRun run = new CalculationRun();
        run.setRunDate(new Date());
        run.setDeceasedId(deceased.getMemberId());

        double totalAssetValue = calculateTotalAssetValue(assets);

        List<HeirCandidate> livingHeirs = determineLivingHeirs(deceased, allFamilyMembers, heirMappings, relationTypes);

        // Apply Blocking Rules (Al-Hajb)
        List<HeirCandidate> eligibleHeirs = applyBlockingRules(livingHeirs);

        // Determine basic shares (Zawil Furuz)
        List<AssetAllocation> allocations = calculateShares(eligibleHeirs, totalAssetValue);

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
                        
                // Find the relation name
                String relationName = relationTypes.stream()
                        .filter(r -> r.getRelationId() == mapping.getRelationId())
                        .map(RelationType::getRelationName)
                        .findFirst().orElse("Unknown");

                if (member != null) {
                    // A person is a living heir if they don't have a dateOfDeath, or if their dateOfDeath 
                    // is AFTER the deceased's dateOfDeath.
                    if (member.getDateOfDeath() == null || 
                       (deceased.getDateOfDeath() != null && member.getDateOfDeath().after(deceased.getDateOfDeath()))) {
                        livingHeirs.add(new HeirCandidate(member, relationName));
                    }
                }
            }
        }
        return livingHeirs;
    }

    private List<HeirCandidate> applyBlockingRules(List<HeirCandidate> candidates) {
        // TODO: Implement Islamic inheritance blocking logic (Al-Hajb).
        // e.g. If any candidate has relationName == "Son", remove candidates with relationName == "Brother"
        return candidates;
    }

    private List<AssetAllocation> calculateShares(List<HeirCandidate> eligibleHeirs, double totalValue) {
        List<AssetAllocation> allocations = new ArrayList<>();
        // TODO: Calculate specific fractions (1/2, 1/4, 1/8, 2/3, 1/3, 1/6) according to Faraid.
        // TODO: Distribute the remaining to Asabah (residuary heirs).
        return allocations;
    }
}
