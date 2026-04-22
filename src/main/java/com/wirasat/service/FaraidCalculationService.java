package com.wirasat.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.wirasat.model.Asset;
import com.wirasat.model.AssetAllocation;
import com.wirasat.model.CalculationRun;
import com.wirasat.model.FamilyMember;

public class FaraidCalculationService {

    /*
     * @param deceased         The family member who passed away.
     * @param allFamilyMembers A list of all known family members.
     * @param assets           The total assets left by the deceased.
     * @return                 A CalculationRun containing the final asset allocations.
     */
    public CalculationRun calculateFaraid(FamilyMember deceased, List<FamilyMember> allFamilyMembers, List<Asset> assets) {
        CalculationRun run = new CalculationRun();
        run.setRunDate(new Date());
        run.setDeceasedId(deceased.getMemberId());

        double totalAssetValue = calculateTotalAssetValue(assets);
        System.out.println("Beginning Faraid calculation for: " + deceased.getName() + ". Total Assets: " + totalAssetValue);

        // Step 1: Filter living heirs
        List<FamilyMember> livingHeirs = determineLivingHeirs(deceased, allFamilyMembers);

        // Step 2: Apply Blocking Rules (Al-Hajb)
        // For example: A son blocks brothers, brothers block uncles, etc.
        List<FamilyMember> eligibleHeirs = applyBlockingRules(livingHeirs);

        // Step 3: Determine basic shares (Zawil Furuz)
        // and residue (Asabah).
        List<AssetAllocation> allocations = calculateShares(eligibleHeirs, totalAssetValue);

        // Step 4: Finalize the run
        run.setNetEstate(java.math.BigDecimal.valueOf(totalAssetValue));
        // You would typically set the allocations to the CalculationRun here, depending on your model
        
        return run;
    }

    private double calculateTotalAssetValue(List<Asset> assets) {
        // Compute total monetary value
        // Note: Replace with actual methods from your Asset model
        return 0.0;
    }

    private List<FamilyMember> determineLivingHeirs(FamilyMember deceased, List<FamilyMember> allFamilyMembers) {
        List<FamilyMember> livingHeirs = new ArrayList<>();
        
        for (FamilyMember member : allFamilyMembers) {
            // A person is a living heir if they don't have a dateOfDeath, or if their dateOfDeath 
            // is AFTER the deceased's dateOfDeath.
            if (member.getMemberId() != deceased.getMemberId()) {
                if (member.getDateOfDeath() == null || 
                   (deceased.getDateOfDeath() != null && member.getDateOfDeath().after(deceased.getDateOfDeath()))) {
                    livingHeirs.add(member);
                }
            }
        }
        return livingHeirs;
    }

    private List<FamilyMember> applyBlockingRules(List<FamilyMember> candidates) {
        // TODO: Implement Islamic inheritance blocking logic (Al-Hajb).
        // Iterate through candidates and remove those who are blocked by closer relatives.
        return candidates;
    }

    private List<AssetAllocation> calculateShares(List<FamilyMember> eligibleHeirs, double totalValue) {
        List<AssetAllocation> allocations = new ArrayList<>();
        // TODO: Calculate specific fractions (1/2, 1/4, 1/8, 2/3, 1/3, 1/6) according to Faraid.
        // TODO: Distribute the remaining to Asabah (residuary heirs).
        return allocations;
    }
}
