package com.example.okquerydsl;

import com.example.okquerydsl.entity.Hello;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static com.example.okquerydsl.entity.QHello.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional // 테스트에 트랜잭션을 적용하면 테스트 이후에는 default로 rollback함
class OkQuerydslApplicationTests {

    @PersistenceContext
    EntityManager em;

    @Test
    void contextLoads() {
        Hello helloEntity = new Hello();
        em.persist(helloEntity);

        JPAQueryFactory queryFactory = new JPAQueryFactory(em);

        Hello result = queryFactory
                .selectFrom(hello)
                .fetchOne();

        assertThat(result.getId()).isEqualTo(helloEntity.getId());
    }

}
