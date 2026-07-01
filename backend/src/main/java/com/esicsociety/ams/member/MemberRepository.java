package com.esicsociety.ams.member;

import com.esicsociety.ams.common.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByAccountNo(String accountNo);

    boolean existsByAccountNo(String accountNo);

    List<Member> findByRoleOrderByNameAsc(Role role);

    List<Member> findAllByOrderByNameAsc();
}
