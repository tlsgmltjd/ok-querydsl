package com.example.okquerydsl.repository;

import com.example.okquerydsl.dto.MemberSearchCondition;
import com.example.okquerydsl.dto.MemberTeamDto;
import com.example.okquerydsl.entity.Member;
import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.example.okquerydsl.entity.QMember.member;
import static com.example.okquerydsl.entity.QTeam.team;
import static org.springframework.util.StringUtils.hasText;

// QuerydslRepositorySupport
// custom repository 구현체에서 위의 추상클래스를 상속받으면 QueryDsl를 편하게 사용할 수 있는 기능들을 지원한다,
// from() 으로 시작해서 querydsl을 사용할 수 있음
// QueryDsl 클래스를 지원해줘서 페이징 관련 가능을 간편하게 사용할 수 있다.
// em 제공

// JPQLQuery<MemberTeamDto> resultQuery = getQuerydsl().applyPagination(pageable, jpaQuery);
// resultQuery.fetchResults();

// queryFactory를 지원하지 않고 스프링 데이터 jpa가 지원하는 sort 기능이 정상적으로 동작하지 않는다.

public class MemberRepositoryImpl extends QuerydslRepositorySupport implements MemberCustomRepository {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        super(Member.class);
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) {

        List<MemberTeamDto> result = from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .select(
                        Projections.constructor(
                                MemberTeamDto.class,
                                member.id,
                                member.username,
                                member.age,
                                team.id,
                                team.name
                        ))
                .fetch();

        return queryFactory
                .select(
                        Projections.constructor(
                                MemberTeamDto.class,
                                member.id,
                                member.username,
                                member.age,
                                team.id,
                                team.name
                        ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(
                        Projections.constructor(
                                MemberTeamDto.class,
                                member.id,
                                member.username,
                                member.age,
                                team.id,
                                team.name
                        ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    public Page<MemberTeamDto> searchPageSimple2(MemberSearchCondition condition, Pageable pageable) {

        JPQLQuery<MemberTeamDto> jpaQuery = from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .select(
                        Projections.constructor(
                                MemberTeamDto.class,
                                member.id,
                                member.username,
                                member.age,
                                team.id,
                                team.name
                        ));

        JPQLQuery<MemberTeamDto> resultQuery = getQuerydsl().applyPagination(pageable, jpaQuery);
        QueryResults<MemberTeamDto> results = resultQuery.fetchResults();

        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(
                        Projections.constructor(
                                MemberTeamDto.class,
                                member.id,
                                member.username,
                                member.age,
                                team.id,
                                team.name
                        ))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe())
                );

//        return new PageImpl<>(content, pageable, total);
        // PageableExecutionUtils.getPage 를 사용해서 count query 함수를 넘기면 count 쿼리를 하지 않아도 되는 상황일 때는 count query를 날리지 않음
        return PageableExecutionUtils.getPage(content, pageable, () -> countQuery.fetchFirst());
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ? team.name.eq(teamName) : null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }
}
