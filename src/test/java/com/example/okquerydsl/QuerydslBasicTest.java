package com.example.okquerydsl;

import com.example.okquerydsl.dto.MemberDto;
import com.example.okquerydsl.dto.QMemberDto;
import com.example.okquerydsl.dto.UserDto;
import com.example.okquerydsl.entity.*;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.okquerydsl.entity.QMember.*;
import static com.example.okquerydsl.entity.QTeam.*;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    void init() {
        initDb();
        queryFactory = new JPAQueryFactory(em);
    }

    @Test
    void jpql() {
        // TODO find member1
        String query = "select m from Member m where m.username = :username";

        Member foundMember = em.createQuery(query, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void queryDsl() {

        // TODO find member1

        String username = "member1";

        /**
         * QClass를 사용할때
         * QMember member = new QMember("m");
         * 이렇게 직접 생성해서 사용할 수 있다.
         * 여기서 생성자로 넣은 값은 JPQL로 만들어질때 alias가 된다.
         */

        Member foundMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq(username))
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo(username);
    }

    @Test
    void search() {
        Member foundMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1")
                                .and(member.age.between(10, 30))
                )
                .fetchOne();

        // eq, ne == (eq().not()), in, notIn, between, goe >=, gt >, loe <=, lt <, like, contains == %v%, startsWith v%

        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() {
        Member foundMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.between(10, 30)
                )
                .fetchOne();

        assertThat(foundMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
//        // 전체조회
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        // 단건조회
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        // 첫 요소만 조회 limit 1 + fetchOne
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();

        // 페이징 정보들을 포함한다, count에 관련된 추가 쿼리 발생
        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .fetchResults();

        List<Member> content = fetchResults.getResults();

        // count 쿼리
//        long count = queryFactory
//                .selectFrom(member)
//                .fetchCount();

    }

    // 1. 나이 내림차순
    // 2. 이름 오름차순
    // 회원이름이 없으면 == null, 마지막에 출력 -> nulls last
    @Test
    void sort() {

        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(
                        member.age.desc(),
                        member.username.asc().nullsLast()
                )
                .fetch();

        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetch();

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        QueryResults<Member> fetchResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0)
                .limit(2)
                .fetchResults();

        assertThat(fetchResults.getResults().size()).isEqualTo(2);
        assertThat(fetchResults.getTotal()).isEqualTo(4);
        assertThat(fetchResults.getLimit()).isEqualTo(2);
        assertThat(fetchResults.getOffset()).isEqualTo(0);
    }

    @Test
    void aggregation() {

        // Tuple을 사용하기보다 DTO로 바로 조회해서 사용하는 것이 좋음
        Tuple result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetchOne();

        assertThat(result.get(member.count())).isEqualTo(4);
        assertThat(result.get(member.age.sum())).isEqualTo(40);
        assertThat(result.get(member.age.avg())).isEqualTo(10);
        assertThat(result.get(member.age.max())).isEqualTo(10);
        assertThat(result.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀 이름과 각 팀의 평균 연령을 구해라
     */
    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .having(team.name.in("teamA", "teamB"))
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(10);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(10);
    }

    // 첫 파라미터에 조인대상, 두번째 파라미터에 Alias
    @Test
    void join() {
        List<Member> members = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(members)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * 세타조인 - 연관관계가 없는 상태에서의 조인
     * -> 회원의 이름과 팀이름이 같은 회원 조회
     */
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    // on절
    // 조인 대상 필터링, 연관관계가 없는 엔티티 외부 조인(세타조인의 외부조인)

    // 회원과 팀을 조인, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                // inner join이면 on절로 필터링 하는것과 Where에서 필터링 하는것과 결과과 동일하다
                // 외부 조인일 때는 On절의 필터링이 의미가 있음 -> 조인 대상을 줄여서 조인해올 수 있음
                // 내부조인이면 가급적 익숙한 where절을 활용해서 조인을 하자
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        // member left outer join team and team.name = teamA
        // tuple = [Member(id=1, username=member1, age=10), Team(id=1, name=teamA)]
        // tuple = [Member(id=2, username=member2, age=10), Team(id=1, name=teamA)]
        // tuple = [Member(id=3, username=member3, age=10), null]
        // tuple = [Member(id=4, username=member4, age=10), null]
    }

    // 연관관계가 없는 엔티티 외부조인
    // 회원의 이름이 팀 이름과 같은 대상 외부조인
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                // .leftJoin(member.team, team) <- 기존 방식
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    // 페치조인
    @Test
    void fetchJoin() {
        em.flush();
        em.clear();

        Member foundMember = queryFactory
                .selectFrom(QMember.member)
                .join(member.team, team).fetchJoin()
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(foundMember.getTeam());
//        assertThat(loaded).as("페치조인 미적용").isFalse();
        assertThat(loaded).as("페치조인 적용").isTrue();

    }

    // sub query
    // JPAExpressions를 사용해서 서브쿼리 가능
    // 나이가 가장 많은 회원 조회
    @Test
    void subQuery() {

        QMember memberSub = new QMember(("memberSub"));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(members).extracting("age")
                .containsExactly(110);
    }

    // 나이가 평균 이상인 회원 조회
    @Test
    void subQueryGoe() {

        QMember memberSub = new QMember(("memberSub"));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(members).extracting("age")
                .containsExactly(110);
    }

    // 나이가 평균 이상인 회원 조회
    @Test
    void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.goe(10))
                ))
                .fetch();

        assertThat(members).extracting("age")
                .containsExactly(10, 10, 10, 10, 110);
    }

    @Test
    void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    // JPA의 JPQL은 from 절의 서브쿼리르 지원하지 않는다. -> 인라인뷰
    // -> 서브쿼리를 join으로 변경이 가능하다면 변경한다 -> 쿼리를 2번으로 분리해서 구현한다 -> nativeSQL을 사용해서 구현한다
    // 너무 화면에 맞춘 쿼리를 날리는것은 비효율적인 상황이 많다. 쿼리 재사용성이 떨어짐

    // case
    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(100).then("백살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(10, 20)).then("10~20살")
                        .when(member.age.between(21, 150)).then("21~150살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // 상수 출력 Expressions.constant()
    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        String result = queryFactory
                // {username}_{age}, .stringValue()로 type cast
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        System.out.println("result = " + result);
    }

    // 중급
    // 프로젝션: select 절에 조회 대상 지정

    @Test
    void signleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tupleProjection() {
        // Tuple은 querydsl의 의존임으로 repository 계층 밖으로 나가는것은 좋지 않다.
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);

            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    void findDtoJpql() {
        // JPQL의 new operation dto projection 조회
        List<MemberDto> resultList = em.createQuery("select new com.example.okquerydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();
    }

    @Test
    void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.bean(
                                MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(
                        // 리플렉션을 써서 필드에 값을 꽂아넣는다.
                        Projections.fields(
                                MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.constructor(
                                MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoUserDto() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(
                        Projections.fields(
                                UserDto.class,

                                member.username.as("name"), // fields 프로젝션 방식은 DTO 객체와 필드명이 다를때는 as로 Alias를 줘야함
//                                ExpressionUtils.as(member.username, "name"),

                                ExpressionUtils.as(
                                        select(memberSub.age.max())
                                        .from(memberSub), "age"
                                ))
                )
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    // @QueryProjection
    // DTO 객체를 Q-Class로 만들어줘서 생성자 필드에 대한 안정성을 보장해준다.
    // -> DTO 객체가 QueryDSL에 대한 라이브러리 의존성이 발생한다.
    @Test
    void findDtoQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    // 동적쿼리
    // 1. BooleanBuilder 2. BooleanExpression

    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder(
                // 초기값 가능
        );

        if (usernameParam != null) {
            builder
                    .and(member.username.eq(usernameParam));
        }

        if (ageParam != null) {
            builder
                    .and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(
                    builder
                )
                .fetch();
    }

    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(
                        // where 절에 null이 들어가면 조건이 무시됨
                        allEq(usernameCond, ageCond)
                )
                .fetch();

    }

    // 메서드를 다른 쿼리에서 재사용이 가능하다.

    // BooleanExpression - 조립 가능
    // 비즈니스상의 여러 검색 조건들을 묶어서 컴포지션이 가능하다.
    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    // 벌크연산
    // 벌크 연산은 영속성 컨텍스트를 신경쓰지 않고 디비에 직접 쿼리가 날라간다.
    // 그렇기 때문에 영속성 컨텍스트와 데이터베이스의 상태가 맞지 않는다.
    // 벌크 연산 이후에는 flush, clear로 영속성 컨텍스트를 비우자
    @Test
    void bulkUpdate() {
        // 영향 받은 row 수
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(99))
                .execute();

        System.out.println("count = " + count);

        em.flush();
        em.clear();
    }

    @Test
    void bulkMath() {
        long count = queryFactory
                .update(member)
                // set A = B
                .set(member.age, member.age.multiply(100))
                .execute();

        System.out.println("count = " + count);
    }

    @Test
    void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.lt(110))
                .execute();

        assertThat(count).isEqualTo(4);
    }

    // sql function
    // 각 디비 dialect에 등록되어있는 함수들을 사용가능하다. (직접 dialect를 상속 받아서 등록해줄수도있다.)

    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate(
//                                "function('replace', {0}, {1}, {2})",
//                                member.username, "member", "M")))

                // 일반적인 ansi 표준 함수들은 Querydsl에서 메서드 체이닝으로 지원해준다.
                // 위와 동일한 작동
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunction2() {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('lower', {0})",
                                member.username))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tset() {
        // JPA에서는 join 대상에 서브쿼리를 넣지 못한다.
        em.createNativeQuery(
                        "SELECT m.* FROM Member m " +
                                "INNER JOIN (SELECT m2.member_id FROM Member m2 WHERE m2.username = 'member1') tb " +
                                "ON m.member_id = tb.member_id", Member.class)
                .getResultList();
    }

    private void initDb() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamA);

        Member member3 = new Member("member3", 10, teamB);
        Member member4 = new Member("member4", 10, teamB);

        Member member5 = new Member("member5", 110);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
        em.persist(member5);
    }
}
