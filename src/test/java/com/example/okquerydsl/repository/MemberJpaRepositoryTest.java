package com.example.okquerydsl.repository;

import com.example.okquerydsl.dto.MemberSearchCondition;
import com.example.okquerydsl.dto.MemberTeamDto;
import com.example.okquerydsl.entity.Member;
import com.example.okquerydsl.entity.Team;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member foundMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(foundMember).isEqualTo(member);

        List<Member> members = memberJpaRepository.findAll_querydsl();
        assertThat(members)
                .containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername_querydsl(member.getUsername());
        assertThat(result2)
                .containsExactly(member);
    }

    @Test
    void search() {

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

        // 조건이 없다면 모든 데이터를 긁어올 수도 있어서 주의가 필요함 (기본조건이나 페이징이 걸려있으면 괜찮다)
        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setAgeGoe(10);
//        condition.setAgeLoe(90);
//        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);
        assertThat(result)
                .extracting("username")
                .containsExactly("member3", "member4");
    }

}
