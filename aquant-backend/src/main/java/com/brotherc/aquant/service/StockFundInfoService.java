package com.brotherc.aquant.service;

import com.brotherc.aquant.entity.StockFundInfo;
import com.brotherc.aquant.model.dto.akshare.FundNameEm;
import com.brotherc.aquant.repository.StockFundInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockFundInfoService {

    private final StockFundInfoRepository stockFundInfoRepository;

    @Transactional(rollbackFor = Exception.class)
    public void saveFundInfos(List<FundNameEm> fundNameEms) {
        if (!CollectionUtils.isEmpty(fundNameEms)) {
            List<StockFundInfo> allExisting = stockFundInfoRepository.findAll();
            Map<String, StockFundInfo> existingMap = allExisting.stream()
                    .collect(Collectors.toMap(StockFundInfo::getFundCode, Function.identity(), (a, b) -> a));

            List<StockFundInfo> toSave = new ArrayList<>();
            for (FundNameEm em : fundNameEms) {
                StockFundInfo info = existingMap.get(em.getFundCode());
                if (info == null) {
                    info = new StockFundInfo();
                    info.setFundCode(em.getFundCode());
                }
                info.setPinyinAbbr(em.getPinyinAbbr());
                info.setFundName(em.getFundName());
                info.setFundType(em.getFundType());
                info.setPinyinFull(em.getPinyinFull());
                toSave.add(info);
            }

            stockFundInfoRepository.saveAll(toSave);
        }
    }

}
