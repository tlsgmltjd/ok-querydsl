package com.example.okquerydsl.repository;

import com.example.okquerydsl.dto.MemberSearchCondition;
import com.example.okquerydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberCustomRepository {
    List<MemberTeamDto> search(MemberSearchCondition condition);
}
