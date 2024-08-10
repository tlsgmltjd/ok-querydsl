package com.example.okquerydsl.repository;

import com.example.okquerydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// MemberRepository <<interface>> -- extends --> JpaRepository <<interface>>, MemberCustomRepository <<interface>> (MemberRepositoryImpl)

public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository {
    List<Member> findByUsername(String username);
}
