package com.hwhub.batch.application.service;

import com.hwhub.batch.domain.enums.ProgramType;
import com.hwhub.batch.domain.repository.HouseholdInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationExpireService {

  private static final long SYSTEM_USER_ID = 2;

  private final HouseholdInvitationRepository repository;

  @Transactional
  public int expireInvitations() {
    return repository.expirePendingInvitations(SYSTEM_USER_ID, ProgramType.BTC_INV_EXPR.getCode());
  }
}
