package com.esicsociety.ams.favourstood;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavourStoodEntryRepository extends JpaRepository<FavourStoodEntry, Long> {

    List<FavourStoodEntry> findByMember_IdAndFinancialYear_IdOrderByEntryDateAscIdAsc(Long memberId, Long yearId);

    List<FavourStoodEntry> findByMember_IdOrderByEntryDateAscIdAsc(Long memberId);
}
