package com.brotherc.aquant.common.repository;

import com.brotherc.aquant.common.entity.SystemCookie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemCookieRepository extends JpaRepository<SystemCookie, Long> {

    SystemCookie findByCookieKey(String cookieKey);

}
