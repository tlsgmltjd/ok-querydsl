package com.example.okquerydsl;

import com.example.okquerydsl.entity.*;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.okquerydsl.entity.QMember.*;
import static com.example.okquerydsl.entity.QTeam.*;
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

    private void initDb() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 10, teamA);

        Member member3 = new Member("member3", 10, teamB);
        Member member4 = new Member("member4", 10, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }
}
