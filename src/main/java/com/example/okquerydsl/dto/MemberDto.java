package com.example.okquerydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection // 생성자도 Q-Class가 생성이 된다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
