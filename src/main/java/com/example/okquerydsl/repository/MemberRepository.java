package com.example.okquerydsl.repository;

import com.example.okquerydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

// MemberRepository <<interface>> -- extends --> JpaRepository <<interface>>, MemberCustomRepository <<interface>> (MemberRepositoryImpl)

// QuerydslPredicateExecutor을 JPARepository에서 extends 받아서 사용할 수 있다.
// SpringDataJpa의 method들 파라미터에 Querydsl의 Q-Class의 where절 조건문을 넘겨서 필터링 하는것이 가능하다.
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository, QuerydslPredicateExecutor<Member> {
    List<Member> findByUsername(String username);
}
