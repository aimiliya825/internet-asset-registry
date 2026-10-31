package com.asset.registry.repository;

import com.asset.registry.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findFirstByRealName(String realName);

    List<SysUser> findAllByOrderByIdAsc();
}
