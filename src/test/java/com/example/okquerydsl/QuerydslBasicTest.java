package com.example.okquerydsl;

import com.example.okquerydsl.entity.Member;
import com.example.okquerydsl.entity.QHello;
import com.example.okquerydsl.entity.QMember;
import com.example.okquerydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static com.example.okquerydsl.entity.QHello.*;
import static com.example.okquerydsl.entity.QMember.*;
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
