package com.brotherc.aquant.repository.sys;

import com.brotherc.aquant.entity.sys.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<SysUser> findByEmail(String email);

}
