package com.wanghao.eldercare.eldercaresystem.mapper.user;

import com.wanghao.eldercare.eldercaresystem.common.*;
import com.wanghao.eldercare.eldercaresystem.common.audit.*;
import com.wanghao.eldercare.eldercaresystem.common.security.*;
import com.wanghao.eldercare.eldercaresystem.common.security.perm.*;
import com.wanghao.eldercare.eldercaresystem.common.security.rbac.*;
import com.wanghao.eldercare.eldercaresystem.common.security.scope.*;
import com.wanghao.eldercare.eldercaresystem.common.ws.*;
import com.wanghao.eldercare.eldercaresystem.controller.user.*;
import com.wanghao.eldercare.eldercaresystem.dto.user.*;
import com.wanghao.eldercare.eldercaresystem.entity.user.*;
import com.wanghao.eldercare.eldercaresystem.service.user.*;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndDeletedAtIsNull(String username);

    Optional<User> findByUserIdAndDeletedAtIsNull(Long userId);

    Optional<User> findFirstByRoleIgnoreCaseAndStatusIgnoreCaseAndDeletedAtIsNullOrderByUserIdAsc(String role, String status);

    List<User> findByRoleIgnoreCaseAndRealNameAndDeletedAtIsNull(String role, String realName);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    boolean existsByUsernameAndUserIdNot(String username, Long userId);

    boolean existsByUsernameAndUserIdNotAndDeletedAtIsNull(String username, Long userId);

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND (:keyword IS NULL
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.realName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:role IS NULL OR LOWER(u.role) = LOWER(:role))
              AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))
            """)
    Page<User> search(@Param("keyword") String keyword,
                      @Param("role") String role,
                      @Param("status") String status,
                      Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND LOWER(u.role) = LOWER(:role)
              AND u.userId IN :userIds
              AND (:keyword IS NULL
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.realName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))
            """)
    Page<User> searchByRoleAndIds(@Param("role") String role,
                                  @Param("userIds") List<Long> userIds,
                                  @Param("keyword") String keyword,
                                  @Param("status") String status,
                                  Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND LOWER(u.role) IN :roles
              AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))
            ORDER BY u.realName ASC, u.username ASC, u.userId ASC
            """)
    List<User> findActiveByRoles(@Param("roles") Collection<String> roles,
                                 @Param("status") String status);

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND LOWER(u.role) IN :roles
              AND (:keyword IS NULL
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.realName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:status IS NULL OR LOWER(u.status) = LOWER(:status))
            """)
    Page<User> searchByRoles(@Param("roles") Collection<String> roles,
                             @Param("keyword") String keyword,
                             @Param("status") String status,
                             Pageable pageable);

    @Modifying
    @Query("""
            UPDATE User u
            SET u.deletedAt = :deletedAt, u.updatedAt = :deletedAt
            WHERE u.userId = :userId AND u.deletedAt IS NULL
            """)
    int softDeleteById(@Param("userId") Long userId, @Param("deletedAt") LocalDateTime deletedAt);
}
