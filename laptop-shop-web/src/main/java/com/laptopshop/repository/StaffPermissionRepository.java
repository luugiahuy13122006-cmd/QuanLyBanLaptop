package com.laptopshop.repository;

import com.laptopshop.entity.AdminModule;
import com.laptopshop.entity.StaffPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffPermissionRepository extends JpaRepository<StaffPermission, Integer> {
    List<StaffPermission> findByAccount_Id(Integer accountId);
    Optional<StaffPermission> findByAccount_IdAndModule(Integer accountId, AdminModule module);
    void deleteByAccount_Id(Integer accountId);
}
