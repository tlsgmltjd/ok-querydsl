package com.example.okquerydsl.repository;

import com.example.okquerydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;

// MemberRepository <<interface>> -- extends --> JpaRepository <<interface>>, MemberCustomRepository <<interface>> (MemberRepositoryImpl)

// QuerydslPredicateExecutor을 JPARepository에서 extends 받아서 사용할 수 있다.
// SpringDataJpa의 method들 파라미터에 Querydsl의 Predicate를 넘겨서 필터링 하는것이 가능하다.
// Predicate: where절 조건문
public interface MemberRepository extends JpaRepository<Member, Long>, MemberCustomRepository, QuerydslPredicateExecutor<Member> {
    List<Member> findByUsername(String username);
}

// QueryDSL에서 Web 관련 기능도 제공한다. @QuerydslPredicate
// 컨트롤러에서 간단하게 파라미터를 요청 받으면 자동으로 querydsl predicate로 바꿔주어 QuerydslPredicateExecutor와 함께 spring data jpa 메서드에 넣어서 바로 원하는 데이터를 조회할 수 있더.
// 하지만 단순한 조건만 가능하며, 다른 계층에서 queryDsl을 의존해야한다는 단점이 있다.
