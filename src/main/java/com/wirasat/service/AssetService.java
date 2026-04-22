package com.wirasat.service;

import com.wirasat.model.Asset;
import com.wirasat.model.AssetType;
import com.wirasat.model.FamilyMember;

import java.util.List;

public interface AssetService {
    List<Asset> getAllAssets();
    List<Asset> searchAssets(String keyword, Integer categoryId);
    void addAsset(Asset asset);
    
    // Supporting methods for lookups
    List<AssetType> getAssetTypes();
    AssetType getAssetTypeById(int typeId);
    
    List<FamilyMember> getPossibleOwners();
    FamilyMember getOwnerById(int ownerId);
}
