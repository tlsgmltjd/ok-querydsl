package com.example.okquerydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryDslConfig {
    // JPAQueryFactory를 빈으로 등록해도 된다.
    // EntityManager는 싱글톤이여도 멀티 스레드 환경에서 동시성 문제가 발생하지 않는다.
    // 각 트랜잭션마다 분리되서 동작하도록 되어있다.
    @Bean
    JPAQueryFactory jpaQueryFactory(EntityManager em) {
        return new JPAQueryFactory(em);
    }
}
