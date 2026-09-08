package com.team2.postservice.fixDeal.service;

import com.team2.postservice.client.UserClient;
import com.team2.postservice.fixDeal.entity.FixDeal;
import com.team2.postservice.fixDeal.repository.FixDealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FixDealService {

    private final FixDealRepository fixDealRepository;

}
