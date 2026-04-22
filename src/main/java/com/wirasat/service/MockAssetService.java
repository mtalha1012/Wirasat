package com.wirasat.service;

import com.wirasat.model.Asset;
import com.wirasat.model.AssetType;
import com.wirasat.model.FamilyMember;

import java.util.List;
import java.util.stream.Collectors;

public class MockAssetService implements AssetService {

    private MockDataService db = MockDataService.getInstance();

    @Override
    public List<Asset> getAllAssets() {
        return db.getAssets();
    }

    @Override
    public List<Asset> searchAssets(String keyword, Integer categoryId) {
        return db.getAssets().stream()
                .filter(a -> (categoryId == null || categoryId == 0 || a.getTypeId() == categoryId))
                .filter(a -> {
                    if (keyword == null || keyword.trim().isEmpty()) return true;
                    String term = keyword.toLowerCase();
                    FamilyMember owner = getOwnerById(a.getOwnerId());
                    String ownerName = owner != null ? owner.getName().toLowerCase() : "";
                    return a.getAssetName().toLowerCase().contains(term) || ownerName.contains(term);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void addAsset(Asset asset) {
        db.addAsset(asset);
    }

    @Override
    public List<AssetType> getAssetTypes() {
        return db.getAssetTypes();
    }

    @Override
    public AssetType getAssetTypeById(int typeId) {
        return db.getAssetTypes().stream().filter(t -> t.getTypeId() == typeId).findFirst().orElse(null);
    }

    @Override
    public List<FamilyMember> getPossibleOwners() {
        return db.getFamilyMembers();
    }

    @Override
    public FamilyMember getOwnerById(int ownerId) {
        return db.getFamilyMembers().stream().filter(o -> o.getMemberId() == ownerId).findFirst().orElse(null);
    }
}
